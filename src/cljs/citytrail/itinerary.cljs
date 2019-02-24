(ns citytrail.itinerary
(:require 
	[reagent.core :as reagent]
)
	)

; atom
(defonce itinerary-data (reagent/atom {
	:header {
		:city "🇫🇷 Paris, France"
		}
	:components [
		{
			:type "start"
			:data {
				:name "Four Seasons Hotel"
				; ...address
			}
		}
		{
			:type "poi"
			:data {
				:name "Eiffel Tower"
				:stay-duration "1 hr"
				;...
			}
		}
		{
			:type "nearby"
			:data {
				:title "Suggested: Best café in Paris"
				:name "La Baguette de France"
				:distance "3 min walk"
				;...
			}
		}
		{
			:type "nearby-seemore"
		}
		{
			:type "poi"
			:data {
				:name "War Museum"
				:stay-duration "2 hrs"
				;...
			}
		}
	]

}))


(defn starting-point [component]
[:div.container-poi
      [:div.container-title "🛏 Starting Point"]
      [:div.poi-name (get-in component [:data :name])]]
 )

(defn point-of-interest [component]
  [:div.container-poi
      [:div.container-title "📸 Point of Interest"]
      [:div.poi-name (get-in component [:data :name])]]
  )

(defn get-component[component]
	(println component)

	(cond
		(= "poi" (get-in component [:type])) 	(point-of-interest component)
		(= "start" (get-in component [:type])) 	(starting-point component)
	)
)

(defn body []
	[:div
		(for [component (get-in @itinerary-data [:components])]
			(get-component component)
		)
	]
)

(defn header []
	[:div.container
		[:div.row
			[:h1.it-title "Itinerary"]
			[:h3.city (get-in @itinerary-data [:header :city])]
		]
	]
)