(ns camels.track-parser
  (:require [camels.game :as game]
            [clojure.string :as str]
            [cljs.reader :as reader]))

(defn coll->keywords [coll]
  (mapv keyword coll))

(defn build-camels [args]
  (let [full-string (str "[" (str/join " " args)"]")
        parsed (reader/read-string full-string)
        keyworded (mapv coll->keywords parsed)]
    keyworded))

(defn build-spaces [camel-tiles]
  (map-indexed #(hash-map :space (inc %1)
                          :camels %2)
               camel-tiles))

(defn pad-track [track]
  (let [track-length (count track)
        new-space-numbers (range (inc track-length) 21)]
    (concat track (mapv game/build-space new-space-numbers))))

(defn build-track [args]
  (let [camel-tiles (build-camels args)
        track (build-spaces camel-tiles)
        padded-track (pad-track track)]
    padded-track))

#_(def args '("[]" "[orange" "blue]" "[green" "white]" "[yellow]" "[]"))
#_(build-track args)
