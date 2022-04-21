(ns discord-games.render
  (:import
   [java.awt Graphics2D Font RenderingHints Color Font]
   [java.awt.image BufferedImage]
   [javax.imageio ImageIO]
   [java.io File]))

(defprotocol TextTileGame
  (draw-game! [game graphics w h])
  (dims [game]))

(defn draw-letter! [^Graphics2D g x y w h letter text-color-code bg-color-code]
  (let [text    (str letter)
        metrics (.getFontMetrics g)
        text-x  (int (+ (/ (- w (.stringWidth metrics text)) 2) x))
        text-y  (int (+ (/ (- h (.getHeight metrics)) 2) (.getAscent metrics) y))]
    (doto g
      (.setColor (Color/decode bg-color-code))
      (.fillRect x y w h)
      (.setColor (Color/decode text-color-code))
      (.drawString text text-x text-y))))

(defn- find-fitting-size [^Graphics2D g w text init-size]
  (loop [size init-size]
    (let [old-font (.getFont g)
          new-font (Font. (.getFontName old-font) Font/PLAIN size)
          _        (.setFont g new-font)
          metrics  (.getFontMetrics g)
          width    (.stringWidth metrics text)]
      (when (>= width w)
        (recur (dec size))))))

(defn draw-text! [^Graphics2D g x y w h text text-color-code bg-color-code]
  (let [old-font (.getFont g)
        _        (find-fitting-size g w text (.getSize old-font))
        metrics  (.getFontMetrics g)
        text-x   (int (+ (/ (- w (.stringWidth metrics text)) 2) x))
        text-y   (int (+ (/ (- h (.getHeight metrics)) 2) (.getAscent metrics) y))]
    (doto g
      (.setColor (Color/decode bg-color-code))
      (.fillRect x y w h)
      (.setColor (Color/decode text-color-code))
      (.drawString ^String text ^int text-x ^int text-y)
      (.setFont old-font))))

(defn render [game-state w h font-size]
  (let [img  (BufferedImage. w h BufferedImage/TYPE_INT_RGB)
        font (Font. "Ubuntu Mono" Font/BOLD font-size)
        g    (.createGraphics img)]
    (doto g
      (.setRenderingHint RenderingHints/KEY_ANTIALIASING RenderingHints/VALUE_ANTIALIAS_ON)
      (.setFont font)
      (#(draw-game! game-state % w h))
      (.dispose))
    ;; (ImageIO/write img "png" (File. "out.png"))
    img))

(defn render-to-file! [text-game ^File file]
  (ImageIO/write ^BufferedImage (apply render text-game (dims text-game)) "png" file))
