(ql:quickload :hunchentoot)
(ql:quickload :flexi-streams)
(ql:quickload :cl-json)

(defpackage :listener-json-api
  (:use :cl :hunchentoot :cl-json))

(in-package :listener-json-api)

(defparameter *notify-storage* "./notifications/")
(defparameter *max-odds* 5)
(defparameter *count-file* "count")

(defun in_post (text) (post-parameter text))
(defun in_get (text) (get-parameter text))

(defun getfile-1 (filename)
  (let (lines)
    (with-open-file (in filename :if-does-not-exist nil)
      (loop for line = (read-line in nil)
	 while line do
	   (push line lines)))
    (if lines
	(reverse lines) nil)))

(defun getfile (filename)
  (format nil "~{~a~%~}" (getfile-1 filename)))

(defun string-join (strings) (format nil "~{~a~^~%~}" strings))

(defun fetch-notification (filename)
  (let ((lines (getfile-1 (concatenate 'string *notify-storage* filename))))
    `((:title . ,(car lines)) (:text . ,(string-join (cdr lines))))))

(defun num2str (num) (format nil "~d" num))

(defun fetch-notifications-by-range (start end)
  (let ((notifications nil))
    (loop
       (when (>= start end) (return))
       (push (fetch-notification (num2str start)) notifications)
       (setq start (1+ start)))
    (reverse notifications)))

(defun setfile (filename text)
  (with-open-file (out filename :direction :output :if-does-not-exist :create :if-exists :supersede)
    (format out "~a" text))
  "success")

(defun print-error (text)
  (format nil (concatenate 'string "{\"error\": \"" text "\"}")))

(defun parse-integer-handled (str)
  (if (null str) nil
      (handler-case (parse-integer str)
	(parse-error ()
	  (progn (format t "Integer parse error") nil)))))

(fetch-notifications-by-range 0 4)

(defun get-what-user-sent ()
  (parse-integer-handled
   (let
       ((post_var (in_post "ts_id"))
	(get_var (in_get "ts_id")))
     (if (not (null post_var)) post_var
	 (if (not (null get_var)) get_var "")))))

(defun get-the-number (var) (if (null var) 0 (if (< var 0) 0 var)))

(defun notifications-count ()
  (get-the-number (parse-integer-handled
		   (car (getfile-1 (concatenate 'string *notify-storage* *count-file*))))))

(defun index ()
  (let
      ((user-number (get-the-number (get-what-user-sent)))
       (notify-count (notifications-count)))

    (when (or (> user-number notify-count) (= user-number 0)) (setf user-number notify-count))
    (when (> (- notify-count user-number) *max-odds*) (setf user-number (- notify-count *max-odds*)))

    (cl-json:encode-json-to-string
     `(("current_ts_id" . ,notify-count)
       ("notifications" . ,(fetch-notifications-by-range user-number notify-count))))))

(setf *dispatch-table* `(,(create-prefix-dispatcher "/" 'index)))
(defvar *acceptor* (make-instance 'easy-acceptor :port 4242))
(start *acceptor*)
(format t "Заходить на http://127.0.0.1:4242")
