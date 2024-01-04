(ns swift-ticketing.model.event-test
  (:require [clojure.test :refer [deftest testing is]]
            [swift-ticketing.fixtures :as fixtures]
            [swift-ticketing.factory :as factory]
            [swift-ticketing.model.event :as event]
            [swift-ticketing.db.event :as db-event]))

(deftest get-events-test
  (testing "Listing Events"
    (let [{:keys [db-spec]} fixtures/test-env
          filters (factory/get-events-params)]
      (with-redefs [db-event/get-events
                    (fn [dbs flts]
                      (when (and (= db-spec dbs)
                                 (= filters flts))
                        :ok))]
        (is (= :ok (event/get-events db-spec filters)))))))

(deftest get-event-test
  (testing "Fetching Event Details"
    (let [{:keys [db-spec]} fixtures/test-env
          event-id (random-uuid)]
      (with-redefs [db-event/get-event-with-tickets
                    (fn [dbs eid]
                      (when (and (= db-spec dbs)
                                 (= event-id eid))
                        :ok))]
        (is (= :ok (event/get-event db-spec event-id)))))))

(deftest create-event-test
  (testing "Creating Event"
    (let [{:keys [db-spec test-user-id]} fixtures/test-env
          create-req (factory/event-request)
          insert-event-called-with-correct-args (atom false)]
      (with-redefs [db-event/insert-event
                    (fn [dbs uid eid req]
                      (when (and (= db-spec dbs)
                                 (= create-req req)
                                 (= test-user-id uid)
                                 (uuid? eid))
                        (reset! insert-event-called-with-correct-args true)))]
        (is (uuid? 
               (event/create-event db-spec test-user-id create-req)))
        (is @insert-event-called-with-correct-args)))))
