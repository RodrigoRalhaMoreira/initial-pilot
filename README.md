# Locally running of besu (from root of project)
```
cd src/besu_local_test
./init_blockchain.sh
```
or simply run:
```
besu --network=dev --miner-enabled --miner-coinbase=0xfe3b557e8fb62b89f4916b721be55ceb828dbd73 --rpc-http-cors-origins="all" --host-allowlist="*" --rpc-ws-enabled --rpc-http-enabled --data-path=/tmp/tmpDatdir
```

# To run the server
Run the class ```TransactionsApiApplication.java```

# To make a request to the server
Run this command on the terminal:
```
curl -X POST http://localhost:8080/api/transactions \
-H "Content-Type: application/json" \
-d '{"type":"Deposit", "amount":1500}'
```

or this one:
```
curl -X POST http://localhost:8080/api/transactions/deploy
```
