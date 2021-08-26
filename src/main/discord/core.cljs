(ns discord.core
  (:require ["discord.js" :as Discord]
            [clojure.string :as str]
            [cljs.core.async :as a :refer [<! >! go put! take! chan to-chan!]]
            [taoensso.timbre.appenders.community.node-spit :as spitter]
            [taoensso.timbre :as timbre
             :refer-macros [log trace debug info warn error fatal report spy get-env]]))

(defonce *client (atom nil))

(defn send-dm! [message]
  (let [{:keys [user-id content]} message]
    (-> (.. ^js @*client -users)
        (.fetch user-id true)
        (.then #(.send % content)))))

(defn send-channel! [message]
  (let [{:keys [channel-id content]} message]
    (-> (.. ^js @*client -channels)
        (.fetch channel-id true)
        (.then #(.send % content)))))

(defn send-message! [message]
  (debug message)
  (condp = (:type message)
    :dm (send-dm! message)
    :channel (send-channel! message)))

(defn send! [messages]
  (if (vector? messages)
    (mapv send-message! messages)
    (send-message! messages)))

(defn build-channel-message [channel-id content]
  {:type :channel
   :channel-id channel-id
   :content content})

(defn error-message
  ([channel-id]
   (build-channel-message channel-id "An error occurred with your message, please try again." ))
  ([channel-id text]
   (error text)
   {:type :channel
    :channel-id channel-id
    :content text}))

(defn send-error! [channel-id]
  (send-message! (error-message channel-id)))

(defn handle-command! [command-processor prefix ^js message]
  (let [body (str/trim (.-content message))
        command-string (subs body (count prefix))
        command-list (str/split command-string #" "+)
        command (first command-list)
        args (rest command-list)
        channel-id (.-channelId message)]
    (debug "Channel id:" channel-id)
    (->> (command-processor command args)
         (map #(build-channel-message channel-id %))
         to-chan!)))

;; Handle messages
(defn message-handler [command-processor prefix ^js message]
  (debug message)
  (try
    (let [body (.-content message)]
      (when (and (not (.. message -author -bot))
                 (str/starts-with? body prefix))
        (let [result (handle-command! command-processor prefix message)]
          (take! result #(do (debug "Result:" %)
                             (send! %))))))
    (catch js/Error e
      (error "Error occurred: " e)
      (send-error! (.. message -channel -id)))))


(defn connect [command-handler prefix token]
  (reset! *client (new Discord/Client (clj->js {:intents [(.. Discord/Intents -FLAGS -GUILDS) (.. Discord/Intents -FLAGS -GUILD_MESSAGES)]})))
  (.on ^js @*client "messageCreate" #(message-handler command-handler prefix %))
  (.login ^js @*client token))

(defn destroy []
  (.destroy ^js @*client))
