(ns replikativ-cljs-demo.core
	(:require [konserve.memory :refer [new-mem-store]]
            [replikativ.peer :refer [client-peer]]
            [replikativ.stage :refer [create-stage! connect! subscribe-crdts!]]
            [replikativ.crdt.cdvcs.realize :refer [stream-into-identity!]]
            [replikativ.crdt.cdvcs.stage :as s]
            [replikativ.crdt.cdvcs.realize :refer [head-value]]
            [cljs.core.async :refer [>! chan timeout]]
            [superv.async :refer [throw-if-exception S]])
  (:require-macros [superv.async :refer [go-try <? go-loop-try]]
                   [cljs.core.async.macros :refer [go-loop]]))

(enable-console-print!)

(def cdvcs-id #uuid "8e9074a1-e3b0-4c79-8765-b6537c7d0c44")

(def uri "ws://127.0.0.1:31744")

(def eval-fns
  {'(fn [_ new] new) (fn [_ new] new)
   '+ +})

(def stream-eval-fns
  {'(fn [_ new] new) (fn [a new] (reset! a new) a)
   '+ (fn [a new] (swap! a + new) a)})

(defn start-local []
  (go-try S
   (let [local-store (<? S (new-mem-store))
         local-peer (<? S (client-peer S local-store))
         stage (<? S (create-stage! "mail:eve@replikativ.io" local-peer))]
     {:store local-store
      :stage stage
      :peer local-peer})))


(defn init []
  (go-try S
   (def client-state (<? S (start-local)))

   (def val-atom (atom -1))
   (stream-into-identity! (:stage client-state)
                          ["mail:eve@replikativ.io" cdvcs-id]
                          stream-eval-fns
                          val-atom)

   (<? S (s/create-cdvcs! (:stage client-state)
                          :description "testing" :id cdvcs-id))
   (add-watch val-atom :print-counter
              (fn [_ _ _ val]
                (set! (.-innerHTML (.getElementById js/document "counter")) val)))

   (<? S (connect! (:stage client-state) uri))))


(defn add! [_]
  (go-try S
   (let [n (js/parseInt (.-value (.getElementById js/document "to_add")))]
     (<? S (s/transact! (:stage client-state)
                      ["mail:eve@replikativ.io" cdvcs-id]
                      [['+ n]]))
     (.info js/console "Current value from store (to check against streaming):"
            (<? S (head-value S (:store client-state)
                              eval-fns
                              ;; manually verify metadata presence
                              (get-in @(:stage client-state)
                                      ["mail:eve@replikativ.io" cdvcs-id :state])))))))


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


  (go-try S (def client-state (<? S (start-local))))

  (go-try S (<? S (connect! (:stage client-state) uri)))

  (go-try S (<? S (subscribe-crdts! (:stage client-state) {"mail:eve@replikativ.io" #{cdvcs-id}})))

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
