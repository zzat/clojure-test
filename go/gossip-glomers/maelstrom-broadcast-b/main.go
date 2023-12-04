package main

import (
	"encoding/json"
	"fmt"
	"log"
	"os"
	"sync"

	maelstrom "github.com/jepsen-io/maelstrom/demo/go"
)

type ServerState struct {
	lock     sync.Mutex
	ids      map[int]bool
	topology map[string][]string
}

type BroadcastRequest struct {
	Type    string `json:"type"`
	Message int    `json:"message"`
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
			topology: make(map[string][]string)}

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
		if serverState.ids[message] == true {
			serverState.lock.Unlock()
			return nil
		}
		serverState.ids[message] = true
		serverState.lock.Unlock()

		response["type"] = "broadcast_ok"
		connectedNodes := serverState.topology[n.ID()]
		if connectedNodes != nil {
			for _, node := range connectedNodes {
        currentNode := node
				fmt.Fprint(os.Stderr, "NodeID: ", node)
				// for _, node := range n.NodeIDs() {
				if node != n.ID() && node != msg.Src {
					go func() {
						n.Send(currentNode, request)
					}()
				}
			}
		}

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
	if err := n.Run(); err != nil {
		log.Printf("Error: %s", err)
		os.Exit(1)
	}
}
