(ns camels.game
  (:require
   [goog.string :as gstring]
   [goog.string.format]))

(def camels
  [:orange
   :green
   :blue
   :white
   :yellow])

(def dice
  [:orange
   :green
   :blue
   :white
   :yellow])

(defn roll
  ([] (inc (rand-int 3)))
  ([n] (inc (rand-int n))))

(defn build-space [n]
  {:space n :camels[]})

(defn build-track [spaces]
  (map build-space (range 1 (inc spaces))))

(defn update-space [track space update-fn]
  (map #(if (= space (:space % ))
          (update-fn %)
          %)
       track))

(defn place-camel [track space camel]
  (update-space track space #(update % :camels conj camel)))

(defn remove-camels [space camels]
  (update space :camels #(remove (set camels) %)))

(defn add-camels [space camels]
  (update space :camels concat camels))

(defn init-game [n]
  {:track (-> (build-track n)
              (place-camel (roll) :orange)
              (place-camel (roll) :green)
              (place-camel (roll) :blue)
              (place-camel (roll) :white)
              (place-camel (roll) :yellow))
   :dice dice})

(defn camel->space [track camel]
  (first
   (filter #(some #{camel} (:camels %))
           track)))

(defn number->space [track n]
  (first
   (filter #(= n (:space %))
           track)))

(defn split-stack [stack camel]
  (split-with (partial not= camel) stack))

(defn move-camel [track camel distance]
  (let [starting-space (camel->space track camel)
        starting-number (:space starting-space)
        [bottom top] (split-stack (:camels starting-space) camel)
        ending-number (+ starting-number distance)]
    (-> track
        (update-space starting-number #(remove-camels % top))
        (update-space ending-number #(add-camels % top)))))

(defn take-turn [game]
  (let [{:keys [track dice]} game
        die (rand-nth dice)
        distance (roll)]
    {:track (move-camel track die distance)
     :dice (remove #{die} dice)}))

(defn play-round [game]
  (loop [state game]
    (if (empty? (:dice state))
      state
      (recur (take-turn state)))))

(defn camel-positions [track]
  (->> track
       (map :camels)
       (apply concat)))

(defn rank-camels [camels]
  (->> (reverse camels)
       (map-indexed #(hash-map %2 (keyword (str (inc %1)))))
       (apply merge)))

(defn tally-camel [scores [camel rank]]
  (update-in scores [camel rank] (fnil inc 0)))

(defn tally-scores [scores track]
  (let [camel->rank (rank-camels (camel-positions track))]
    (reduce #(tally-camel %1 %2) scores camel->rank)))

(defn run-sim [track times]
  (let [game {:track track :dice dice}]
    (loop [x 0
           scores {}]
      (if (>= x times)
        scores
        (recur (inc x)
               (tally-scores scores (:track (play-round game))))))))

(defn ranks->percentage [ranks runs]
  (->> (/ ranks runs)
      (* 100)
      float
      (gstring/format "%.2f")))

(defn summarize-score [camel-ranks runs]
  (into {} (for [[k v] camel-ranks]
             (when (= k :1)
               [k (ranks->percentage v runs)]))))

(defn get-winners [track runs]
  (let [results (run-sim track runs)]
    (into {} (for [[k v] results]
               [k (summarize-score v runs)]))))

(defn play-game
  ([]
   (play-game (:track (init-game 20))))
  ([track]
   (let [game {:track track :dice dice}
         results (get-winners (:track game) 1000)]
     [(map :camels track) results])))
