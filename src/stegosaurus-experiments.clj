(use 'conexp.main)

;;;

(require '[conexp.contrib.exec :as exec])
(require '[clojure.java.io :as io])
(import  '[java.util Calendar])
(import  '[java.text SimpleDateFormat])

(defn points-to-file
  "Puts points into a temporary file and return the resulting file-name.  The resulting
  file is suitable for gnuplot to be used as a data file."
  [points]
  (let [file-name (let [^Calendar cal (Calendar/getInstance)
                        ^SimpleDateFormat sdf (SimpleDateFormat. "'data/'yyyy-MM-dd'T'HH:mm:ssz'.dat'")]
                    (.format sdf (.getTime cal)))]
    (with-open [^java.io.Writer out (io/writer file-name)]
      (doseq [point points]
        (.write out (str (nth point 0) " " (nth point 1) "\n"))))
    file-name))

(defn plot-points-from-file
  "Given a file-name that is assumed to be a data file suitable for gnuplot, generate a
  jpeg file of this plot and show it using geeqie."
  [file-name]
  (let [image-name  (str file-name ".jpg")
        gnuplot-cmd (str "set terminal jpeg;"
                         "set output \\\"" image-name"\\\";"
                         "plot \\\"" file-name "\\\" with points;")]
    (exec/run-in-shell "sh" "-c" (format "echo \"%s\" | gnuplot" gnuplot-cmd))
    (exec/run-in-shell "geeqie" image-name)
    image-name))

;;;

(require '[clojure.core.reducers :as r]
         '[conexp.contrib.algorithms :as a])

(defn compute-stegosaurus-points
  "Given a sequence of formal contexts K, compute the sequence of pairs (number of intents
  of K, number of pseudo-intents of K)."
  [random-contexts]
  (r/fold 128
          concat
          (fn [points ctx]
            (conj points
                  [(count (a/concepts :next-closure ctx))
                   (count (a/canonical-base ctx))]))
          (vec random-contexts)))

(defn show-stegosaurus
  "Generates a picture of the stegosaurus and shows it.  It generates no-samples many
  random contexts by calling the nullary function random-context-fn that often.  From
  these random contexts, the number of intents and pseudo-intents are computed and then
  displayed."
  [no-samples random-context-fn]
  (-> (r/fold concat
              (fn [coll _]
                (conj coll (reduce-context (random-context-fn))))
              (vec (range no-samples)))
      compute-stegosaurus-points
      points-to-file
      plot-points-from-file))

;;;

(comment

  (show-stegosaurus 10000 #(random-context (rand-int 500) 10 (rand 1)))
  (show-stegosaurus 5000 #(random-context (rand-int 500) (rand-int 10) (rand 1)))
  (show-stegosaurus 5000 #(loop []
                            (let [ctx (reduce-context (random-context (rand-int 500) 10 (rand 1)))]
                              (if (= 10 (count (attributes ctx)))
                                ctx
                                (recur)))))

  'comment)

;;;
