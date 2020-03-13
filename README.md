# chatbot
A simple chatbot which serves multi-clients using TCP-IP protocol.
First run the MulticonnectServer on the computer you decide to be the server. This server should always be on.
Now one client can connect to this server by running the MulticonnectClient file on their own computer, create an account and log in to see if someone is online. If there is one, these clients can make a line seperated from the server(i.e. this line will hold even if the server is down). Such line will be down when one of the clients close their window.
