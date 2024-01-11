(ns swift-ticketing-ui.theme)

(def input-class
  (str "block w-full max-w-full rounded-md border-0 py-1.5 text-gray-900 "
       "shadow-sm ring-1 ring-inset ring-gray-300 placeholder:text-gray-400 "
       "focus:ring-2 focus:ring-inset focus:ring-indigo-600 "
       "sm:max-w-xs sm:text-sm sm:leading-6"))

(def button-base-class
  (str "rounded-md border border-transparent "
       "px-4 py-2 text-base font-medium text-white shadow-sm "
       "hover:bg-indigo-700 focus:outline-none focus:ring-2 "
       "focus:ring-indigo-500 focus:ring-offset-2"))

(def primary-button-class
  (str "bg-indigo-600 " button-base-class))

(def danger-button-class
  (str "bg-red-600 " button-base-class))
