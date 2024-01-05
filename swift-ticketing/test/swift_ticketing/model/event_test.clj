(ns swift-ticketing.model.event-test
  (:require [clojure.test :refer [deftest testing is]]
            [swift-ticketing.fixtures :as fixtures]
            [swift-ticketing.factory :as factory]
            [swift-ticketing.model.event :as event]
            [swift-ticketing.db.event :as db-event]))

(deftest get-events-test
  (testing "Listing Events"
    (let [{:keys [db-spec]} fixtures/test-env
          filters (factory/get-events-params)
          get-events-args (atom {})
          results [(factory/events-result)]]
      (with-redefs
       [db-event/get-events
        (fn [dbs flts]
          (reset! get-events-args {:db-spec dbs
                                   :filters flts})
          results)]
        (is (= results (event/get-events db-spec filters)))
        (is (= {:db-spec db-spec
                :filters filters}
               @get-events-args))))))

(deftest get-event-test
  (testing "Fetching Event Details"
    (let [{:keys [db-spec]} fixtures/test-env
          event-id (random-uuid)
          get-event-with-tickets-args (atom {})
          result (factory/event-with-tickets event-id)]
      (with-redefs
       [db-event/get-event-with-tickets
        (fn [dbs eid]
          (reset! get-event-with-tickets-args {:db-spec dbs
                                               :event-id eid})
          result)]
        (is (= result (event/get-event db-spec event-id)))
        (is (= {:db-spec db-spec
                :event-id event-id}
               @get-event-with-tickets-args))))))

(deftest create-event-test
  (testing "Creating Event"
    (let [{:keys [db-spec test-user-id]} fixtures/test-env
          create-req (factory/event-request)
          insert-event-args (atom {})]
      (with-redefs
       [db-event/insert-event
        (fn [dbs uid eid req]
            (reset! insert-event-args
                    {:db-spec dbs
                     :user-id uid
                     :create-request req})
            eid)]
        (is (uuid?
             (event/create-event db-spec test-user-id create-req)))
        (is (= {:db-spec db-spec
                :user-id test-user-id
                :create-request create-req}
               @insert-event-args))))))
