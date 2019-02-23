(ns ^:figwheel-no-load citytrail.dev
  (:require
    [citytrail.core :as core]
    [devtools.core :as devtools]))

(devtools/install!)

(enable-console-print!)

(core/init!)
