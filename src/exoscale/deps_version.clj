(ns exoscale.deps-version
  (:require [clojure.string :as str]
            [clojure.spec.alpha :as s]))

(def default-opts
  #:exoscale.deps-version{:file "VERSION"
                          :default "0.1.0-SNAPSHOT"
                          :rx "^(?i)(?:(\\d+)\\.)(?:(\\d+)\\.)(\\d+)(?:\\-(.+))?"})

(s/def :exoscale.deps-version/file string?)
(s/def :exoscale.deps-version/default string?)
(s/def :exoscale.deps-version/key simple-keyword?)
(s/def :exoscale.deps-version/suffix (s/nilable string?))
(s/def :exoscale.deps-version/suffix-fn ifn?)

(s/def :exoscale.deps-version/opts
  (s/keys :req [:exoscale.deps-version/file
                :exoscale.deps-version/default
                :exoscale.deps-version/rx
                (or :exoscale.deps-version/key
                    (or :exoscale.deps-version/suffix
                        :exoscale.deps-version/suffix-fn))]))

(defn parse-version [rx s]
  (let [[[_ major minor patch suffix]] (re-seq rx s)]
    (cond-> #:exoscale.deps-version{:major (parse-long (or major 0))
                                    :minor (parse-long (or minor 0))
                                    :patch (parse-long (or patch 0))}
      suffix
      (assoc :exoscale.deps-version/suffix suffix))))

(defn read-version-file
  ([] (read-version-file default-opts))
  ([opts]
   (let [{:exoscale.deps-version/keys [default file]} (into default-opts opts)]
     (try
       (slurp file)
       (catch java.io.FileNotFoundException _
         default)))))

(defn read-version [{:as opts :exoscale.deps-version/keys [rx]}]
  (parse-version (re-pattern rx)
                 (read-version-file opts)))

(defn version-string
  [{:as version-map :exoscale.deps-version/keys [suffix]}]
  (cond-> (str/join "." ((juxt
                          :exoscale.deps-version/major
                          :exoscale.deps-version/minor
                          :exoscale.deps-version/patch)
                         version-map))
    (string? suffix)
    (str "-" suffix)))

(defn write-version-file
  [version-map {:exoscale.deps-version/keys [file]}]
  (spit file (version-string version-map)))

(defn inc-version
  [version-map k]
  (update version-map k inc))

(defn update-suffix
  [version-map f]
  (update version-map :exoscale.deps-version/suffix f))

(defn bump-version* [opts]
  (let [{:exoscale.deps-version/keys [key suffix suffix-fn] :as opts}
        (->> opts
             (into default-opts)
             (s/assert :exoscale.deps-version/opts))
        version-map (read-version opts)
        version-map (if (keyword? key)
                      (inc-version version-map
                                   (keyword "exoscale.deps-version" (name key)))
                      version-map)
        new-version (cond-> version-map
                      ;; only update suffix is it's persent in opt map
                      (contains? opts :exoscale.deps-version/suffix)
                      (update-suffix (fn [_] suffix))

                      ;; could be used as a lib (ex via build file)
                      (ifn? suffix-fn)
                      (update-suffix suffix-fn))]
    (write-version-file new-version opts)
    new-version))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn bump-version
  [& {:as opts}]
  (bump-version* (into {}
                       (map (fn [[k v]]
                              [(keyword "exoscale.deps-version" (name k)) v]))
                       opts)))

;; (bump-version :file "/home/mpenet/code/instancepool/VERSION"
;;               :key :patch
;;               :suffix "SNAPSHOT")
