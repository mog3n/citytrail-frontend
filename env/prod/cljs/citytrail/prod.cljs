(ns citytrail.prod
  (:require [citytrail.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
