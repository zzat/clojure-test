package main

import (
	"encoding/json"
	"log"
	"os"
	"sync"

	maelstrom "github.com/jepsen-io/maelstrom/demo/go"
)

type ServerState struct {
	lock sync.Mutex
	ids  []int
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

func main() {
	n := maelstrom.NewNode()
	serverState := ServerState{}

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
		serverState.ids = append(serverState.ids, message)
		serverState.lock.Unlock()

		response["type"] = "broadcast_ok"

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
		response["messages"] = serverState.ids
		serverState.lock.Unlock()

		return n.Reply(msg, response)

	})

	n.Handle("topology", func(msg maelstrom.Message) error {
		// var request TopologyRequest
    response := make(map[string]any)
		// if err := json.Unmarshal(msg.Body, &request); err != nil {
		// 	return err
		// }

		response["type"] = "topology_ok"
		//   serverState.lock.Lock()
		// body["messages"] = serverState.ids
		//   serverState.lock.Unlock()
		// log.Printf("LOG!!!:  %v", response)
		return n.Reply(msg, response)

	})
	if err := n.Run(); err != nil {
		log.Printf("Error: %s", err)
		os.Exit(1)
	}
}
