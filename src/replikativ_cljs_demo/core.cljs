(ns replikativ-cljs-demo.core
	(:require [konserve.memory :refer [new-mem-store]]
            [replikativ.peer :refer [client-peer]]
            [replikativ.stage :refer [create-stage! connect! subscribe-crdts!]]
            [replikativ.crdt.cdvcs.realize :refer [stream-into-atom!]]
            [replikativ.crdt.cdvcs.stage :as s]
            [cljs.core.async :refer [>! chan timeout]]
            [full.cljs.async :refer [throw-if-throwable]])
  (:require-macros [full.cljs.async :refer [go-try <? go-loop-try]]
                   [cljs.core.async.macros :refer [go-loop]]))

(enable-console-print!)

(def cdvcs-id #uuid "8e9074a1-e3b0-4c79-8765-b6537c7d0c44")

(def uri "ws://127.0.0.1:31744")

(def eval-fns
  {'(fn [_ new] new) (fn [_ new] new)
   '+ +})

(defn start-local []
  (go-try
   (let [local-store (<? (new-mem-store))
         err-ch (chan)
         local-peer (<? (client-peer local-store err-ch))
         stage (<? (create-stage! "mail:eve@replikativ.io" local-peer err-ch))
         _ (go-loop [e (<? err-ch)]
             (when e
               (.log js/console "ERROR:" e)
               (recur (<? err-ch))))]
     {:store local-store
      :stage stage
      :error-chan err-ch
      :peer local-peer})))


(defn init []
  (go-try
   (def client-state (<? (start-local)))

   (def val-atom (atom -1))
   (stream-into-atom! (:stage client-state)
                      ["mail:eve@replikativ.io" cdvcs-id]
                      eval-fns
                      val-atom)
   (add-watch val-atom :print-counter
              (fn [_ _ _ val]
                (set! (.-innerHTML (.getElementById js/document "counter")) val)))

   (try
     (<? (connect! (:stage client-state) uri))
     ;; this waits until the remote CDVCS is available
     (<? (subscribe-crdts! (:stage client-state) {"mail:eve@replikativ.io" #{cdvcs-id}}))
     ;; alternatively create a local copy with the same initialization
     ;; as the server, but then you can commit against an outdated
     ;; (unsynchronized) version, inducing conflicts
     (catch js/Error e
       (<? (s/create-cdvcs! (:stage client-state) :description "testing" :id cdvcs-id))
       (<? (s/transact (:stage client-state)
                       ["mail:eve@replikativ.io" cdvcs-id]
                       '(fn [_ new] new)
                       0))
       (<? (s/commit! (:stage client-state) {"mail:eve@replikativ.io" #{cdvcs-id}}))))))


(defn add! [_]
  (go-try
   (let [n (js/parseInt (.-value (.getElementById js/document "to_add")))]
     (<? (s/transact (:stage client-state)
                     ["mail:eve@replikativ.io" cdvcs-id]
                     '+
                     n)))
   (<? (s/commit! (:stage client-state) {"mail:eve@replikativ.io" #{cdvcs-id}}))))


(defn main [& args]
  (init)
  (set! (.-onclick (.getElementById js/document "add")) add!))

(comment
  ;; start REPL with
  ;; 1. `lein figwheel` on a shell
  ;; 2. open browser on corresponding port to activate cljs REPL
  ;; 3. connect to nREPL on port 7888
  ;; 4. paste into REPL to hook into figwheel cljs REPL (e.g. in CIDER),
  ;;    there might be a better way...
  (require 'figwheel-sidecar.repl-api)
  (figwheel-sidecar.repl-api/cljs-repl)


  (go-try (def client-state (<? (start-local))))

  (go-try (<? (connect! (:stage client-state) uri)))

  (go-try (<? (subscribe-crdts! (:stage client-state) {"mail:eve@replikativ.io" #{cdvcs-id}})))

  (keys (get @(:stage client-state) "mail:eve@replikativ.io"))

  (println (-> client-state :stage deref :config))

  (println (-> client-state :log deref))

  (-> client-state :store :state deref (get ["mail:eve@replikativ.io" cdvcs-id]) :state :commit-graph)


  (->> client-state :store :state deref keys (filter vector?))

  (-> client-state :store :state deref (get ["mail:eve@replikativ.io" cdvcs-id]) :state :commit-graph count)

  (go-try
   (<? (s/transact (:stage client-state)
                   ["mail:eve@replikativ.io" cdvcs-id]
                   '(fn [old params] params)
                   999)))


  (-> client-state :stage deref (get-in ["mail:eve@replikativ.io" cdvcs-id :prepared]) println)

  (println (:stage client-state))

  (go-try
   (<? (s/commit! (:stage client-state) {"mail:eve@replikativ.io" #{cdvcs-id}})))
  )
