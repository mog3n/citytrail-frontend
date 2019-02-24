(ns citytrail.core
  (:require
   [ajax.core :as ajax]
   [reagent.core :as reagent :refer [atom]]
   [reagent.session :as session]
   [reitit.frontend :as reitit]
   [clerk.core :as clerk]
   [accountant.core :as accountant]
   [citytrail.itinerary :as itinerary]
   ))

;; -------------------------
;; Routes

(def router
  (reitit/router
   [["/" :index]
    ["/items"
     ["" :items]
     ["/:item-id" :item]]
    ["/about" :about]
    ["/itinerary" :itinerary]
    ]))

(defn path-for [route & [params]]
  (if params
    (:path (reitit/match-by-name router route params))
    (:path (reitit/match-by-name router route))))

(path-for :about)

;; -------------------------
;; Page components

;; App Data -------------------------------------------------------------------
(defonce form-info (reagent/atom {:start-point              nil
                                  :point-of-interest-editor nil}))

(defonce app-info (reagent/atom {:show?                  false
                                 :start-point            nil
                                 :start-point-id         []
                                 :start-point-extra      nil
                                 :points-of-interest     []
                                 :points-of-interest-ids []
                                 :extra-places           nil}))

(defonce itinerary-info (reagent/atom {}))

;; Handlers -------------------------------------------------------------------
(defn start-point-handler [data]
  (swap! app-info assoc :start-point            data
                        :start-point-id         [(get-in data [:places 0 :place_id])]
                        :start-point-extra      nil
                        :points-of-interest     []
                        :points-of-interest-ids []
                        :extra-places           nil))

(defn point-of-interest-handler [data]
  (swap! app-info update :points-of-interest #(conj % data))
  (swap! app-info update :points-of-interest-ids #(conj % (get-in data [:places 0 :place_id]))))

(defn error-handler [{:keys [status status-text]}]
  (js/alert (str status " - " status-text)))

(defn start-point-extra-handler [data]
  (swap! app-info assoc :start-point-extra data))

(defn extra-places-handler [data]
  (swap! app-info assoc :extra-places data))

;; Requests -------------------------------------------------------------------
(defn load-point [point handler]
  (ajax/POST "https://my-project-1550937209990.appspot.com/location/search"
             {:params          {:query point}
              :handler         handler
              :error-handler   error-handler
              :format          :json
              :response-format :json
              :keywords?       true}))

(defn parse-poi-extra [{:keys [place nearby]}]
  (swap! itinerary-info update :components #(conj % {:type "poi"
                                                     :data {:name (:name place)}}))
  (swap! itinerary-info update :components #(conj % {:type "nearby"
                                                     :data {:name (get-in nearby [0 :name])}}))
  (swap! itinerary-info update :components #(conj % {:type "nearby"
                                                     :data {:name (get-in nearby [1 :name])}})))

(defn load-itinerary-data []
  (reset! itinerary-info {:header {} :components []})
  (swap! itinerary-info assoc :header {:city (str (get-in @app-info [:start-point :places 0 :city]) ", " (get-in @app-info [:start-point :places 0 :country]))})
  (swap! itinerary-info update :components #(conj % {:type "start"
                                                     :data {:name (get-in @app-info [:start-point :places 0 :name])
                                                            :address (get-in @app-info [:start-point :places 0 :formatted_address])}}))
  (swap! itinerary-info update :components #(conj % {:type "nearby"
                                                     :data {:name (get-in @app-info [:start-point-extra 0 :nearby 0 :name])}}))
  (swap! itinerary-info update :components #(conj % {:type "nearby"
                                                     :data {:name (get-in @app-info [:start-point-extra 0 :nearby 1 :name])}}))
  (doseq [poi-extra (:extra-places @app-info)]
    (parse-poi-extra  poi-extra)))

(defn load-extra-places [place-ids handler fin]
  (ajax/POST "https://my-project-1550937209990.appspot.com/location/compute"
             {:params          {:locations place-ids}
              :handler         handler
              :error-handler   error-handler
              :format          :json
              :response-format :json
              :keywords?       true
              :finally         fin}))

;; Forms ----------------------------------------------------------------------
(defn start-point-form []
  [:div
    [:input
      {:type "text"
      :placeholder "Four Seasons Hotel, Paris"
       :on-change #(swap! form-info assoc :start-point (-> % .-target .-value))}]
    [:button.button
      {:on-click #(load-point (:start-point @form-info) start-point-handler)}
      "Verify"]])

(defn remove-poi []
  (swap! app-info update :points-of-interest #(vec (drop-last %)))
  (swap! app-info update :points-of-interest-ids #(vec (drop-last %))))

(defn display-points-of-interest []
  [:div.poi-list
    (for [poi (:points-of-interest @app-info)]
      [:div.poi-item (get-in poi [:places 0 :name])])])

(defn points-of-interest-form []
  [:div
    [:input
      {:type "text"
      :placeholder "Eiffel Tower"
       :on-change #(swap! form-info assoc :point-of-interest-editor (-> % .-target .-value))}]
    [:button.button
      {:on-click #(load-point (:point-of-interest-editor @form-info) point-of-interest-handler)}
      "Add"]
      (display-points-of-interest)

    (if (not-empty (:points-of-interest @app-info))
        [:button.button.red
          {:on-click #(remove-poi)}
          "Remove"])
    ]
  )


(defn create-itinerary-data-fn []
  (load-extra-places (:start-point-id @app-info) start-point-extra-handler
  #(load-extra-places (:points-of-interest-ids @app-info) extra-places-handler load-itinerary-data)))

(defn create-itinerary-data []
  [:a {:href (path-for :itinerary)}
    [:button.button.btn-go
      {:on-click #(create-itinerary-data-fn)}
      "Go"]])

;; Home Page and Extra --------------------------------------------------------
(defn point-of-interest [data]
  [:div
    [:row
      [:h4 "📷 Point of Interest"]
      [:h1 (get-in data [:data :name])]]])

(defn display-app-info []
  (if (:show? @app-info)
    [:div
      [:div (str @form-info)] [:hr]
      [:div (str @app-info)] [:hr]
      [:div (str @itinerary-info)] [:hr]]))

(defn toggle-app-info []
  [:button.btn
    {:on-click #(swap! app-info update :show? not)}
    "Toggle App Info"])

(defn home-page []
  (fn []
    [:span.main
    ;[:a {:href (path-for :itinerary)}
      ;[:button.btn "Launch Itinerary"]]

      [:img.logo {:src "https://storage.googleapis.com/www.unclejoesfamilyrestaurant.com/clojure/citytrail.png"}]
      [:div.container
        [:div.label "Where will you be staying?"]
         [start-point-form]
         [:div.label "Points of Interest"]
         [points-of-interest-form]
         [create-itinerary-data]
         ;[toggle-app-info] [:hr]
         ;[display-app-info]
         ;[:div "Itinerary Data: " @itinerary-info] [:hr]]
         ]]
       ))

(defn items-page []
  (fn []
    [:span.main
     [:h1 "The items of citytrail"]
     [:ul (map (fn [item-id]
                 [:li {:name (str "item-" item-id) :key (str "item-" item-id)}
                  [:a {:href (path-for :item {:item-id item-id})} "Item: " item-id]])
               (range 1 60))]]))

(defn item-page []
  (fn []
    (let [routing-data (session/get :route)
          item (get-in routing-data [:route-params :item-id])]
      [:span.main
       [:h1 (str "Item " item " of citytrail")]
       [:p [:a {:href (path-for :items)} "Back to the list of items"]]])))

(defn about-page []
  (fn [] [:span.main
          [:h1 "About citytrail"]]))

(defn itinerary-page []
  (fn [] [:div
          [itinerary/header itinerary-info]
          [itinerary/body itinerary-info]
        ]
    )
)

;; -------------------------
;; Translate routes -> page components

(defn page-for [route]
  (case route
    :index #'home-page
    :about #'about-page
    :items #'items-page
    :item #'item-page
    :itinerary #'itinerary-page))

;; -------------------------
;; Page mounting component

(defn current-page []
  (fn []
    (let [page (:current-page (session/get :route))]
      [:div
       ;[:header
        ;[:p [:a {:href (path-for :index)} "Home"] " | "
         ;[:a {:href (path-for :about)} "About citytrail"]]]
       [page]
       [:footer.footer
        [:p "CityTrail ❤"]]
         ])))

;; -------------------------
;; Initialize app

(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (clerk/initialize!)
  (accountant/configure-navigation!
   {:nav-handler
    (fn [path]
      (let [match (reitit/match-by-path router path)
            current-page (:name (:data  match))
            route-params (:path-params match)]
        (reagent/after-render clerk/after-render!)
        (session/put! :route {:current-page (page-for current-page)
                              :route-params route-params})
        (clerk/navigate-page! path)
        ))
    :path-exists?
    (fn [path]
      (boolean (reitit/match-by-path router path)))})
  (accountant/dispatch-current!)
  (mount-root))
