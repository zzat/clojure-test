package main

import (
	"encoding/json"
	"log"
	"os"
	"sync"
  "context"
  "time"

	maelstrom "github.com/jepsen-io/maelstrom/demo/go"
)

type ServerState struct {
	lock sync.Mutex
}

type AddRequest struct {
	Type    string `json:"type"`
	Delta int    `json:"delta"`
}

type ReadRequest struct {
	Type    string `json:"type"`
}

type ReadResponse struct {
	Type    string `json:"type"`
  Value   int    `json:"value"`
}

func main() {
	n := maelstrom.NewNode()
  kv := maelstrom.NewSeqKV(n)

	serverState := ServerState{}

	n.Handle("add", func(msg maelstrom.Message) error {
		var request AddRequest
    response := make(map[string]any)
		if err := json.Unmarshal(msg.Body, &request); err != nil {
			return err
		}

		serverState.lock.Lock()
    ctx, cancel := context.WithTimeout(context.Background(), time.Second)
    defer cancel()
		serverState.lock.Unlock()

		response["type"] = "add_ok"

		return n.Reply(msg, response)
	})

	n.Handle("read", func(msg maelstrom.Message) error {
		var request ReadRequest
    response := make(map[string]any)
    ctx, cancel := context.WithTimeout(context.Background(), time.Second)
    defer cancel()

		if err := json.Unmarshal(msg.Body, &request); err != nil {
			return err
		}

    val, err := kv.ReadInt(ctx, n.ID())

    if err != nil {
      return err
    }

		response["type"] = "read_ok"
    response["value"] = val

		return n.Reply(msg, response)

	})

	if err := n.Run(); err != nil {
		log.Printf("Error: %s", err)
		os.Exit(1)
	}
}
