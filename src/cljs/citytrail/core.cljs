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

(defonce app-info (reagent/atom {:start-point        nil
                                 :points-of-interest []
                                 :extra-places       nil}))

;; Handlers -------------------------------------------------------------------
(defn start-point-handler [data]
  (do
    (swap! app-info assoc :start-point data :points-of-interest [])
    (swap! form-info assoc :points-of-interest [])))

(defn point-of-interest-handler [data]
  (swap! app-info update :points-of-interest #(conj % data)))

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

(defn load-pois [pois]
  (ajax/POST "https://my-project-1550937209990.appspot.com/location/compute"
             {:params          {:locations pois}
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
  (swap! app-info assoc :points-of-interest [])
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

;; Home Page and Extra --------------------------------------------------------
(defn point-of-interest [data]
  [:div
    [:row
      [:h4 "📷 Point of Interest"]
      [:h1 (get-in data [:data :name])]]])

(defn home-page []
  (fn []
    [:span.main
     [:h1 "Welcome to CityTrail"]
     ;[:ul
      ;[:li [:a {:href (path-for :items)} "Items of citytrail"]]
      ;[:li [:a {:href "/borken/link"} "Borken link"]]]
     [start-point-form]
     [:hr]
     [points-of-interest-form]
     [display-points-of-interest]
     [:hr]
     [:div (str @form-info)]
     [:hr]
     [:div (str @app-info)]
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