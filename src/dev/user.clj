(ns user
  (:require
    [app.sys :as sys]
    [clojure.tools.namespace.repl :as tools-ns :refer [set-refresh-dirs refresh]]))

;; Ensure we only refresh the source we care about. This is important
;; because `resources` is on our classpath and we don't want to
;; accidentally pull source from there when cljs builds cache files there.
(set-refresh-dirs "src/dev" "src/main")

(defonce sys (atom nil))

(defn start []
  (if @sys
    "System already running."
    (do
      (reset! sys (sys/start))
      ;; Return nil to avoid printing the whole system in repl.
      nil)))

(defn stop []
  (sys/stop @sys)
  (reset! sys nil))

(defn restart
  "Stop the system, reload all source code, then restart the system.

  See documentation of tools.namespace.repl for more information."
  []
  (stop)
  (refresh :after 'user/start))

(comment
  (start)
  (restart))
