(ns xvsy.datasets
  (:require [korma.core]
            [schema.core :as s]
            [xvsy.goog-bq :as goog-bq]
            [xvsy.ggsql :refer [defdataset]]))

(korma.db/defdb bq (goog-bq/goog-bq {}))

(defdataset bq-natality "natality"
  (korma.core/table "publicdata:samples.natality")
  (korma.core/database bq)
  (assoc :dataset "samples")
  (assoc :cols {"alcohol_use"  {:factor true :type s/Bool}
                "apgar_1min" {:factor false :type s/Int}
                "apgar_5min" {:factor false :type s/Int}
                "born_alive_alive" {:factor false :type s/Int}
                "born_alive_dead" {:factor false :type s/Int}
                "born_dead" {:factor true :type s/Int}
                "child_race" {:factor true :type s/Int}
                "cigarettes_per_day" {:factor false :type s/Int}
                "cigarette_use" {:factor true :type s/Bool}
                "day" {:factor true :type s/Int}
                "drinks_per_week" {:factor false :type s/Int}
                "ever_born" {:factor false :type s/Int}
                "father_age" {:factor false :type s/Int}
                "father_race" {:factor true :type s/Int}
                "gestation_weeks" {:factor false :type s/Int}
                "is_male" {:factor true :type s/Bool}
                "lmp" {:factor true :type s/Str}
                "month" {:factor true :type s/Int}
                "mother_age" {:factor false :type s/Int}
                "mother_birth_state" {:factor true :type s/Str}
                "mother_married" {:factor true :type s/Bool}
                "mother_race" {:factor true :type s/Int}
                "mother_residence_state" {:factor true :type s/Str}
                "plurality" {:factor false :type s/Int}
                "record_weight" {:factor false :type s/Int}
                "source_year" {:factor true :type s/Int}
                "state" {:factor true :type s/Str}
                "wday" {:factor true :type s/Int}
                "weight_gain_pounds" {:factor false :type s/Int}
                "weight_pounds" {:factor false :type s/Num}
                "year" {:factor true :type s/Int}}))
