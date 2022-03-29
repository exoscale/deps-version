(ns exoscale.deps-version
  (:require [clojure.edn :as edn]
            [clojure.string :as str]))

(def default-opts #:exoscale.deps-version{:file "version.edn"})

(defn read-version
  ([] (read-version (:exoscale.deps-version/file default-opts)))
  ([version-file]
   (prn "reading " version-file)
   (try (edn/read-string (slurp version-file))
        (catch java.io.FileNotFoundException _
          #:exoscale.deps-version{:major 0
                                  :minor 1
                                  :patch 0
                                  :suffix "SNAPSHOT"
                                  :raw "0.1.0-SNAPSHOT"}))))

(defn write-version [version-map version-file]
  (spit version-file (pr-str version-map)))

(def rx #"^(?i)(?:(\d+)\.)(?:(\d+)\.)(\d+)(?:\-(.+))?")

(defn parse-version [s]
  (let [[[_ major minor patch suffix]] (re-seq rx s)]
    (cond-> #:exoscale.deps-version{:major (Long/parseLong (or major 0))
                                    :minor (Long/parseLong (or minor 0))
                                    :patch (Long/parseLong (or patch 0))}
      suffix
      (assoc :exoscale.deps-version/suffix suffix))))

(defn add-version-string
  [{:as version-map :exoscale.deps-version/keys [suffix]}]
  (assoc version-map
         :exoscale.deps-version/raw
         (cond-> (str/join "." ((juxt
                                 :exoscale.deps-version/major
                                 :exoscale.deps-version/minor
                                 :exoscale.deps-version/patch)
                                version-map))
           (string? suffix)
           (str "-" suffix))))

(defn bump-version
  [version-map k]
  (update version-map k inc))

(defn update-suffix
  [version-map f]
  (update version-map :exoscale.deps-version/suffix f))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn bump [opts]
  (let [{:exoscale.deps-version/keys [file key suffix suffix-fn] :as opts} (merge default-opts opts)
        current-version (read-version file)
        current-version (if (keyword? key)
                          (bump-version current-version
                                        (keyword "exoscale.deps-version" (name key)))
                          current-version)
        new-version (cond-> current-version
                      ;; only update suffix is it's persent in opt map
                      (contains? opts :exoscale.deps-version/suffix)
                      (update-suffix (fn [_] suffix))

                      ;; could be used as a lib (ex via build file)
                      (ifn? suffix-fn)
                      (update-suffix suffix-fn)

                      :then
                      (add-version-string))]
    (write-version new-version file)
    new-version))

(comment
  (parse-version "1.0.0-alpha1-SNAPSHOT")
  (parse-version "1.0.0-alpha1")
  (parse-version "1.0.0-alpha")
  (parse-version "1.0.0-alpha-SHAPSHOT")

  (bump-version (parse-version "1.0.0-alpha1-SNAPSHOT")
                :exoscale.deps-version/major)

  (bump-version (parse-version "1.0.0-alpha1-SNAPSHOT")
                :exoscale.deps-version/minor)

  (bump-version (parse-version "1.0.0-alpha1-SNAPSHOT")
                :exoscale.deps-version/patch)

  (update-suffix (parse-version "1.0.0-alpha1-SNAPSHOT")
                 (constantly nil))

  (bump :exoscale.deps-version {:key nil :suffix nil})
  (bump :exoscale.deps-version {:key :patch :suffix "SNAPSHOT"}))

;;
