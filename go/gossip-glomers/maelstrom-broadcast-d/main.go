package main

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"os"
	"sync"
	"time"

	maelstrom "github.com/jepsen-io/maelstrom/demo/go"
)

type ServerState struct {
	lock           sync.Mutex
	ids            map[int]bool
	broadcastQueue []int
	topology       map[string][]string
}

type BroadcastRequest struct {
	Type            string `json:"type"`
	Message         *int   `json:"message,omitempty"`
	BatchedMessages []int  `json:"batched_messages,omitempty"`
}

type ReadRequest struct {
	Type string `json:"type"`
}

type TopologyRequest struct {
	Type     string              `json:"type"`
	Topology map[string][]string `json:"topology"`
}

func getKeys(m map[int]bool) []int {
	ids := make([]int, 0, len(m))
	for k := range m {
		ids = append(ids, k)
	}
	return ids
}

func main() {
	n := maelstrom.NewNode()
	serverState :=
		ServerState{ids: make(map[int]bool),
			topology:       make(map[string][]string),
			broadcastQueue: make([]int, 0)}

	var wg sync.WaitGroup

	n.Handle("broadcast_ok", func(msg maelstrom.Message) error {
		return nil
	})

	n.Handle("broadcast", func(msg maelstrom.Message) error {
		var request BroadcastRequest
		response := make(map[string]any)
		if err := json.Unmarshal(msg.Body, &request); err != nil {
			return err
		}

		serverState.lock.Lock()
		message := request.Message
		batchedMessages := request.BatchedMessages
		if message != nil {
			if serverState.ids[*message] == true {
				serverState.lock.Unlock()
				return nil
			}
			serverState.ids[*message] = true
			serverState.broadcastQueue = append(serverState.broadcastQueue, *message)
			serverState.lock.Unlock()
		} else {
			if batchedMessages != nil {
				for _, msg := range batchedMessages {
					serverState.ids[msg] = true
				}
			}
			serverState.lock.Unlock()
		}

		response["type"] = "broadcast_ok"
		// connectedNodes := serverState.topology[n.ID()]
		// if connectedNodes != nil {
		// 	for _, node := range connectedNodes {
		// }

		return n.Reply(msg, response)
	})

	n.Handle("read", func(msg maelstrom.Message) error {
		var request ReadRequest
		response := make(map[string]any)
		if err := json.Unmarshal(msg.Body, &request); err != nil {
			return err
		}

		response["type"] = "read_ok"
		serverState.lock.Lock()
		response["messages"] = getKeys(serverState.ids)
		serverState.lock.Unlock()

		return n.Reply(msg, response)

	})

	n.Handle("topology", func(msg maelstrom.Message) error {
		var request TopologyRequest
		response := make(map[string]any)
		if err := json.Unmarshal(msg.Body, &request); err != nil {
			return err
		}

		response["type"] = "topology_ok"
		serverState.lock.Lock()
		serverState.topology = request.Topology
		serverState.lock.Unlock()
		// log.Printf("LOG!!!:  %v", response)
		return n.Reply(msg, response)

	})

  err := n.Run()
	if err != nil {
		log.Printf("Error: %s", err)
		os.Exit(1)
	}

	for {
		time.Sleep(4 * time.Millisecond)
		go func() {
			serverState.lock.Lock()
			currentBatch := serverState.broadcastQueue
			serverState.broadcastQueue = make([]int, 0)
			serverState.lock.Unlock()
			for _, node := range n.NodeIDs() {
				// fmt.Fprint(os.Stderr, "NodeID: ", node)
				if node != n.ID() && node != msg.Src {
					currentNode := node
					wg.Add(1)
					go func() {
						ctx, cancel := context.WithTimeout(context.Background(), 2*time.Second)
						defer cancel()
						for {
							// if res := n.Send(currentNode, request); res != nil {
							// if _, res := n.SyncRPC(ctx, currentNode, request); res != nil {
							if _, res := n.SyncRPC(ctx, currentNode, BroadcastRequest{Type: "broadcast", BatchedMessages: currentBatch}); res != nil {
								fmt.Fprint(os.Stderr, "SyncRPC: ", res)
								continue
							}
							wg.Done()
							break
						}
					}()
				}
			}
		}()

    if err == nil {
      break
    }
	}

	wg.Wait()
}
