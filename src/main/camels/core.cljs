(ns camels.core
  (:require [camels.config :as config]
            [camels.game :as sim]
            [discord.core :as discord]
            [taoensso.timbre.appenders.community.node-spit :as spitter]
            [taoensso.timbre :as timbre
             :refer-macros [log trace debug info warn error fatal report spy get-env]]))

(defonce prefix "l^^P ")
(defonce *debug-message (atom nil))

(defn help-message []
  "Commands:
'l^^P random' - Generates a new random track. Displays the track and the probabilities.
'l^^P [] [orange blue] [green white] [yellow]' - Calculates the odds for the given track.")

(defn command-processor [command args]
  (debug "Command:" command)
  (debug "Args:" args)
  (condp = command
    "random" [(str (sim/play-game))]
    "help" [(help-message)]
    [(help-message)]))

(defn connect []
  (discord/connect command-processor prefix config/token))

(defn ^:dev/after-load reload! []
  (debug "Code Reloaded")
  (discord/destroy)
  (connect))

(defn configure-logging [log-level]
  (timbre/set-level! log-level)
  (timbre/merge-config! {:appenders
                         {:spit (spitter/node-spit-appender
                                 {:fname "log.txt"
                                  :append? true})}}))

(defn -main []
  (configure-logging :debug)
  (info "Starting bot...")
  (connect))
