# Distributed-Hash-Tables
<p>
A simple distributed hash table implementation for peer to peer systems.
DHT is a centralized distributed systems algorithm used for look-up services in peer to peer systems(P2P). In this, any participating
node can look-up up value for any key stored on any of the participating peers.
This implemenetation uses a ring structure of peers to look-up keys in the network. 
</p>
<p>
In order to properly simlulate nework conditions, I have written the algorithm on Android platform using 5 Android Virtual Machines(AVDs).
Each AVD has its's own ID and port number. These AVDs can communicate with each other via TCP sockets.
</p>
<p>
  I have leveraged the content-provider utility provided in Android SDK to write the core DHT algorithm. All the implementation of DHT is written as a content provider.
 </p>
<img src="https://www.researchgate.net/profile/Matteo_Mordacchini/publication/252132570/figure/fig20/AS:298111160340489@1448086647329/An-example-of-a-Chord-Overlay-Networksource-58.png"></img>
I have implemented following features of DHT:<br>
<ol>
  <li> <b>Node Joins:</b></li>
  <p> A node can join the DHT by issuing a join request to the master node. Master node status is assigned to the AVD with ID = 5554. It is assumed that master node already present in the network when the ring formation starts. Position of a new joining node in the ring is determined by comparing the hash code of ID of new node. I have used SHA-1 as hashing algorithm. Upon arrival of a new node join request, the master node determines the successor and predecessor nodes of the arriving peer and communicates information.</p>
  <li> <b>Consistent Hashing and Load Balancing:</b></li>
  <p>
  The goal of DHT is to store and balance the laod across all peers in the network. This load balancing is done using consistent hashing. When a new key-value insertion request arrives at any peer, the appropriate position of the key in DHT ring is decided by comparing SHA-1 hashes of keys with hashes of node IDs. A key is stored at a particular node only if hash of key is less than or equal to the hash of node ID. The hashes are compared by lexicographical string comparison. 
  </p>

  <li><b>Basic CRUD Operations:</b></li>
  <p>
  For every query() and delete(), the program is able to recognize two special strings for the selection
parameter.
<ol>
  <li>
  If * ( not including quotes, i.e., “*” should be the string as input) is given as the
  selection parameter to query(), then the query returns all <key, value> pairs
  stored in entire DHT.
  </li>
  <li>
  Similarly, if * is given as the selection parameter to delete(), the query
  deletes all <key, value> pairs stored in entire DHT.
  </li>
  <li>If @ ( not including quotes, i.e., “@” should be the string as input) is given as
the selection parameter to query() on an AVD, then it returns all <key,
value> pairs stored in your local partition of the node , i.e., all <key, value> pairs
stored locally in the AVD on which you run query().
</li>
  <li>
  Similarly, if @ is given as the selection parameter to delete() on an AVD, then
it deletes all <key, value> pairs stored in local partition of the node ,
i.e., all <key, value> pairs stored locally in the AVD on which you run delete().
  </li>
  <li>
  If insert() or update() is called at any node, then the key-value is inserted at the correct partiton node according to DHT algorithm.
  </li>
</ol> 

  </p>
</ol>
