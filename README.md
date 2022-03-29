# Simplistic version manipulation tool (tools.deps) for clojure

* reads `version.edn` from file (if exists) as map
* updates part(s) of the version map 
* writes file back and return version map

Parsed data output map :

`#:exoscale.deps-version{:major 1 :minor 2 :patch 3 :suffix "SNAPSHOT" :raw "1.2.3-SNAPSHOT"}`

## Usage

Add to your deps 

```clj
{:deps {org.clojure/clojure {:mvn/version "1.11.0"}}
 :paths ["src"]
 :aliases
 {:deps-modules {:deps {exoscale/deps-version {:git/sha "..."
                                               :git/url "git@github.com:exoscale/deps-version.git"}}
                 :ns-default exoscale.deps-version}}}
```

```terminal
clj -Ttools install exoscale/deps-version '{:git/sha "" :git/url "git@github.com:exoscale/deps-version.git"}' :as version
```

Then you can just use it by running:

```shell
clj -Tversion bump '#:exoscale.deps-version{:key :patch}'
clj -Tversion bump '#:exoscale.deps-version{:key :major}'
clj -Tversion bump '#:exoscale.deps-version{:key :minor}'
clj -Tversion bump '#:exoscale.deps-version{:key :patch :suffix "SNAPSHOT"}'
clj -Tversion bump '#:exoscale.deps-version{:suffix nil}'
```

or simply as a library
