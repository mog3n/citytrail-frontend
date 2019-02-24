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
                                  :point-of-interest-editor nil
                                  :points-of-interest       []}))

(defonce app-info (reagent/atom {:start-point            nil
                                 :points-of-interest     []
                                 :points-of-interest-ids []
                                 :extra-places           nil}))

(defonce itinerary-info (reagent/atom {}))

;; Handlers -------------------------------------------------------------------
(defn start-point-handler [data]
  (do
    (swap! app-info assoc :start-point data :points-of-interest [] :points-of-interest-ids [] :extra-places nil)
    (swap! form-info assoc :points-of-interest [])))

(defn point-of-interest-handler [data]
  (swap! app-info update :points-of-interest #(conj % data))
  (swap! app-info update :points-of-interest-ids #(conj % (get-in data [:places 0 :place_id]))))

(defn error-handler [{:keys [status status-text]}]
  (js/alert (str status " - " status-text)))

;; Requests -------------------------------------------------------------------
(defn load-point [point handler]
  (ajax/POST "https://my-project-1550937209990.appspot.com/location/search"
             {:params          {:query point}
              :handler         handler
              :error-handler   error-handler
              :format          :json
              :response-format :json
              :keywords?       true}))

(defn load-extra-places [place-ids]
  (ajax/POST "https://my-project-1550937209990.appspot.com/location/compute"
             {:params          {:locations (str place-ids)}
              :handler         #(swap! app-info assoc :extra-places %)
              :error-handler   error-handler
              :format          :json
              :response-format :json
              :keywords?       true}))

;; Forms ----------------------------------------------------------------------
(defn start-point-form []
  [:div
    [:input
      {:type "text"
       :on-change #(swap! form-info assoc :start-point (-> % .-target .-value))}]
    [:button.btn
      {:on-click #(load-point (:start-point @form-info) start-point-handler)}
      "Load Starting Point"]])

(defn add-poi []
  (swap! form-info update :points-of-interest #(conj % (:point-of-interest-editor @form-info))))

(defn remove-poi []
  (swap! form-info update :points-of-interest #(vec (drop-last %))))

(defn display-points-of-interest []
  [:div
    (for [poi (:points-of-interest @form-info)]
      [:p poi])])

(defn load-points-of-interest []
  (swap! app-info assoc :points-of-interest [] :points-of-interest-ids [] :extra-places nil)
  (doseq [poi (:points-of-interest @form-info)]
    (load-point poi point-of-interest-handler)))

(defn points-of-interest-form []
  [:div
    [:input
      {:type "text"
       :on-change #(swap! form-info assoc :point-of-interest-editor (-> % .-target .-value))}]
    [:button.btn
      {:on-click #(add-poi)}
      "Add Point of Interest"]
    [:button.btn.btn-danger
      {:on-click #(remove-poi)}
      "Remove Point of Interest"]
    [:button.btn
      {:on-click #(load-points-of-interest)}
      "Load Points of Interest"]])

(defn get-extra-places-button []
  [:button.btn
    {:on-click #(load-extra-places (:points-of-interest-ids @app-info))}
    "Load Extra Places"])

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
  (doseq [poi-extra (:extra-places @app-info)]
    (parse-poi-extra  poi-extra)))

(defn create-itinerary-data []
  [:button.btn.btn-primary
    {:on-click #(load-itinerary-data)}
    "Load Itinerary Data"])

;; Home Page and Extra --------------------------------------------------------
(defn point-of-interest [data]
  [:div
    [:row
      [:h4 "📷 Point of Interest"]
      [:h1 (get-in data [:data :name])]]])

(defn home-page []
  (fn []
    [:span.main
    [:a {:href (path-for :itinerary)}
      [:button.btn "Launch Itinerary"]
    ]
     [:h1 "Welcome to CityTrail"]
     ;[:ul
      ;[:li [:a {:href (path-for :items)} "Items of citytrail"]]
      ;[:li [:a {:href "/borken/link"} "Borken link"]]]
     [start-point-form]
     [:hr]
     [points-of-interest-form]
     [display-points-of-interest]
     [:hr]
     [get-extra-places-button]
     [:hr]
     [create-itinerary-data]
     [:hr]
     [:div (str @form-info)]
     [:hr]
     [:div (str @app-info)]
     [:hr]
     [:div (str @itinerary-info)]
     [:hr]]))

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
      [itinerary/header]
      [itinerary/body]
    ])
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
       ;[:footer
        ;[:p "citytrail was generated by the "
         ;[:a {:href "https://github.com/reagent-project/reagent-template"} "Reagent Template"] "."]]
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
