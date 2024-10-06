(ns c3pro-editor.core
  (:import (at.ac.c3pro.node Message Event Interaction)
           (at.ac.c3pro.chormodel MultiDirectedGraph Role ChoreographyModel)
           (org.jbpt.graph Fragment)
           (java.util HashSet)))

; instantiating a simple message
(.toString (Message. "this one is a msg" "id1"))

; instantiating a MultiDirectedGraph (which depends on jbpt) + delete test case
(let [m1            (Message. "book_trip")
      m2            (Message. "check cash")
      traveler      (Role. "Traveler")
      travelAgency  (Role. "TravelAgency")
      i1            (Interaction. "i1" traveler travelAgency m1)
      i2            (Interaction. "i2" travelAgency traveler m2)
      start         (Event. "start")
      end           (Event. "end")
      choreoGraph   (doto (MultiDirectedGraph.)
                      (.addEdge start i1)
                      (.addEdge i1    i2)
                      (.addEdge i2    end))
      choreoRpst    (ChoreographyModel. choreoGraph)
      ;; complex delete case start
      fragToDelete  (.getFragmentWithSource choreoRpst i1)
      fragToDelete' (.getFragmentWithTarget choreoRpst i1)
      fragSet       (doto (HashSet.)
                      (.add fragToDelete)
                      (.add fragToDelete'))
      choreoRpst'   (.delete choreoRpst fragSet)
      ;; complex delete case stop
      ; the same node-based delete with the simple API
      choreoRpst''  (.delete choreoRpst i1)
      ]
  [choreoRpst' choreoRpst'']
  (println (.getGraph (Fragment. choreoGraph))))

