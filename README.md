# To run the server
Run the class ```TransactionsApiApplication.java```

# To make a request to the server
Run this command on the terminal:
```
curl -X POST http://localhost:8080/api/transactions \
-H "Content-Type: application/json" \
-d '{"type":"Deposit", "amount":1500}'
```
