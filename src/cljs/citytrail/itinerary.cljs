(ns citytrail.itinerary
(:require
	[reagent.core :as reagent]
)
	)

; atom
(defonce itinerary-data (reagent/atom {
	:header {
		:city "🇫🇷 Paris, France"
		:starting-date "Feb 21, 2018 - 10:00am"
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


(defn draw-timestamp [component]
	[:div.timeline-container
		[:div.timestamp-container
		[:span.timestamp
			(get-in @itinerary-data [:header :starting-date])
			]
		]
		[:div.timeline-line]
	])

(defn draw-timeline [component]
	[:div.timeline-container
		;[:div.timeline-line]
		;[:div.timeline-time (get-in component [:data :time])]
		[:div.timeline-line]
	]
)

(defn starting-point [component]
[:div.container-poi
      [:div.container-title "🛏 Starting Point"]
      [:div.poi-name (get-in component [:data :name])]]
 )

(defn point-of-interest [component]
	[:div
		  [:div.timeline-line]
		  [:div.container-poi
	      [:div.container-title "📸 Point of Interest"]
	      [:div.poi-name (get-in component [:data :name])]]
	]
  )


(defn recommended [component]
	[:div
		  [:div.left-thing]
		  [:draw-timestamp ""]
		  [:div.container-poi.recommended-container
	      [:div.container-title.recommended-title "Recommended"]
	      ;[:div.container-title.recommended-title (get-in component [:data :title])]
	      [:div.poi-name.recommended-name (get-in component [:data :name])]]
	]
)

(defn see-more [component]
	[:div
		[:div.left-thing]
		[:div.btn-container
			"See more recommendations"
		]
	]
)

(defn get-component[component]


	(cond
		(= "poi" (get-in component [:type])) 				(point-of-interest component)
		(= "start" (get-in component [:type])) 				(starting-point component)
		(= "timeline" (get-in component [:type]))			(draw-timeline component)
		(= "nearby" (get-in component [:type]))				(recommended component)
		(= "nearby-seemore" (get-in component [:type]))		(see-more component)
	)
)

(defn body [thedata]
	[:div
		[draw-timestamp]
		(for [component (get-in @thedata [:components])]
			[get-component component]
		)
	]
)

(defn header [thedata]
	[:div.container.table
		(println thedata)
		[:div.table-cell
			[:div.row.inline-block
				[:h1.it-title "Itinerary"]
				[:h3.city (get-in @thedata [:header :city])]
		]
		]
	]
)
