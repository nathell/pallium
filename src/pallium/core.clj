(ns pallium.core
  (:require [gloss.core :as gloss]
            [gloss.io :as gio]
            [nio.core :as nio])
  (:import (java.nio ByteBuffer)))

(gloss/defcodec cinterp (gloss/repeated [:int32-le :int32-le]
                                        :prefix (gloss/prefix
                                                 :int32-le
                                                 #(/ % 8)
                                                 #(* % 8))))

(defn load-dict [prefix fmt]
  (let [img (nio/mmap (str prefix ".image") nil nil :mode :read-only)
        ofs (nio/mmap (str prefix ".offset") nil nil :mode :read-only)
        offsets (gio/decode-all :int32-le ofs)
        decode (fn [offset]
                 (.position img (- offset 4))
                 (if (= fmt :string)
                   (let [len (gio/decode :int32-le img false)]
                     (.position img offset)
                     (gio/decode (gloss/string :utf-8 :length (dec len))
                                 (.slice img)
                                 false))
                   (gio/decode fmt img false)))]
    (mapv decode offsets)))

(defn load-corpus [prefix]
  (let [orth (load-dict (str prefix ".poliqarp.orth") :string)
        base (load-dict (str prefix ".poliqarp.base1") :string)
        tag (load-dict (str prefix ".poliqarp.tag1") :string)
        interp (load-dict (str prefix ".poliqarp.interp1") cinterp)]
    {:orth orth, :base base, :tag tag, :interp interp}))

(comment
  (def c (load-corpus "/Users/nathell/corpora/nkjp_1m_1.2/nkjp1M")))
