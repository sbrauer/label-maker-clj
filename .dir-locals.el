;; Emacs' Directory Variables - https://www.gnu.org/software/emacs/manual/html_node/emacs/Directory-Variables.html
;; http://endlessparentheses.com/a-quick-guide-to-directory-local-variables.html
;; CIDER clojure cli aliases - https://github.com/clojure-emacs/cider/issues/2396

;; intent here is to ensure CIDER always starts in the dev profile.
;; when targeting non-dev environments, you may want to comment this out
((nil . ((cider-clojure-cli-global-options . "-A:dev"))))
