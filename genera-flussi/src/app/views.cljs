(ns app.views
  (:require [app.state :refer [app-state app-generated censimento app-setting]]
            [app.events :refer [increment decrement]]
            [cljsjs.semantic-ui-react]
            [reagent.core :refer [atom]]
            [app.tracciato :refer [genera-carta-n download-trc]]))

(def semantic-ui js/semanticUIReact)

(defn component
  "Get a component from sematic-ui-react:
    (component \"Button\")
    (component \"Menu\" \"Item\")"
  [k & ks]
  (if (seq ks)
    (apply goog.object/getValueByKeys semantic-ui k ks)
    (goog.object/get semantic-ui k)))

;;to-do remove unused components
(def container        (component "Container"))
(def button           (component "Button"))
(def segment          (component "Segment"))
(def dimmer           (component "Dimmer"))
(def loader           (component "Loader"))
(def message          (component "Message"))
(def message-header   (component "Message" "Header"))
(def form             (component "Form"))
(def form-field       (component "Form" "Field"))
(def table            (component "Table"))
(def table-header     (component "Table" "Header"))
(def table-body       (component "Table" "Body"))
(def table-footer     (component "Table" "Footer"))
(def table-row        (component "Table" "Row"))
(def table-headercell (component "Table" "HeaderCell"))
(def table-cell       (component "Table" "Cell"))
(def header           (component "Header"))
(def grid             (component "Grid"))
(def grid-column      (component "Grid" "Column"))
(def grid-row         (component "Grid" "Row"))
(def divider          (component "Divider"))
(def dropdown         (component "Dropdown"))
(def input            (component "Input"))



(def patterns
  {:tdate "[0-9]{2}[^\\d][0-9]{2}[^\\d][0-9]{4}(;[0-9]{2}[^\\d][0-9]{2}[^\\d][0-9]{4}){0,1}"
   :tnumber "\\d+(;\\d+)*"})

(defn posfield
  [satom lname pname dominio tipo obbligatorio specifiche code]
  (let [pattern (tipo patterns)
        req (= "O" obbligatorio)]
    ^{:key lname}
    [:> grid-column 
     [:> form-field 
      [:label lname]
      (if (empty? dominio)
        [:input {:placeholder pname
                 :value (code @satom)
                 :pattern pattern
                 :title specifiche
                 :required req
                 :on-change (fn [e]
                              (swap! satom assoc code (-> e .-target .-value)))
                 :key pname}]
        [:select.ui.dropdown {:on-change (fn [e] (swap! satom assoc code (-> e .-target .-value)))}
         (for [d dominio]
           [:option {:value d 
                     :key d} d])])]]))

(defn inputati
  [items]
  (filter #(:inputato %) items))

(defn content
  [items s]
  [:> grid {:columns 2 :stackable true}
   (for [{:keys [nome descrizione dominio tipo obbligatorio specifiche code]} (inputati items)]
     ^{:key nome}
     [posfield s nome descrizione dominio tipo obbligatorio specifiche code])
   [:> grid-row {:columns 1}
   [:> grid-column 
    [:> button {:type "submit" 
                :color "blue"
                :on-click (fn [evt]
                            (let [x (:num @app-setting)
                                  new-records (genera-carta-n x @s)]
                              (reset! app-generated new-records)))} "Submit"]]]])

(defn form-tracciato
  [campi]
  (let [specs (first campi)
        s (atom (reduce #(assoc %1 (:code %2) (or (first (:dominio %2)) "")) {} specs) )];;(inputati specs)
    (fn []
      [:> form
       [content specs s]])))

(defn tabella-generata
  [records testata]
  [:> table {:celled true}
   [:> table-header
    [:> table-row
       (for [testa testata]
         ^{:key (random-uuid)}
         [:> table-headercell testa ])]]
   [:> table-body ; to-do sort by using nth number from state
    (for [riga (sort-by #(nth % 5) records)]
      ^{:key (random-uuid)}
      [:> table-row
       (for [campo riga]
         ^{:key (random-uuid)}
         [:> table-cell campo])])]
   [:> table-footer]])

(defn nomi-tracciati
  [klist]
  (map #({:key % :value % :text %}) klist))

(defn app []
  [:div ; to-do use re-frame 
   [:> container
    [:> header {:content "Generatore di Tracciati" :textAlign "center" :as "h1"}]
    [:> grid {:columns 2 :stackable true}
      [:> grid-row
       [:> grid-column
        [:select.ui.dropdown ; to-do select tracciato from dropdown
         (for [trc (vals @censimento)]
           [:option {:value (:name trc) :key (:name trc)} (:name trc)])]]
        [:> grid-column
         [:> input { :placeholder (:num @app-setting)
                    :on-change (fn [e]
                                 (swap! app-setting assoc :num (js/parseInt (-> e .-target .-value))))}]]]]
    [:> divider]
    [form-tracciato (vals @app-state)]
    [:> divider]
    [:> grid {:columns 1 :stackable true}
     [:> grid-row
      [:> grid-column {:style {:overflow-x "scroll"}}
       [tabella-generata (vals @app-generated) (map #(:nome %) (first (vals @app-state)))]]]
     [:> grid-row
      [:> grid-column
       [:> button {:color "blue"
                   :on-click (fn [evt]
                               (download-trc (str (.getTime (js/Date.)) ".csv") (vals @app-generated)))} "Export"]]]]]])
