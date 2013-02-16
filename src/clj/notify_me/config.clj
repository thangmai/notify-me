(ns notify-me.config)

(def opts {:port 5000
           :database-url "postgresql://localhost:5432/notify-me"
           :temp-dir "/tmp"
           :tts-command "/Users/guilespi/Documents/Development/interrupted/google-tts/text2mp3.sh"})
