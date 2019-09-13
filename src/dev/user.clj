(ns user
  (:require
   [rp.syringe.core :as syringe]
   [clojure.tools.namespace.repl :as tools-ns :refer [set-refresh-dirs refresh]]))

;; Ensure we only refresh the source we care about. This is important
;; because `resources` is on our classpath and we don't want to
;; accidentally pull source from there when cljs builds cache files there.
(set-refresh-dirs "src/dev" "src/main")

(def ^:dynamic *env*
  "Make it easy to change the env at the REPL."
  :dev)

(defn start
  "Start system stored in the var #'rp.syringe.core/system"
  []
  (if (syringe/system-running?)
    :already-started
    (let [env *env*]
      (println "starting system in env: " env)
      (syringe/start-application!
       (syringe/build-system env "config.edn"))
      :started)))

(def go start)

(defn stop
  "Stop system stored in the var #'rp.syringe.core/system"
  []
  (if (syringe/system-running?)
    (do (syringe/stop-system!)
        :stopped)
    :already-stopped))

(defn system
  "Return the syringe system."
  []
  syringe/system)

(defn reset
  []
  (when (syringe/system-running?)
    (stop))
  (refresh :after 'user/go))
