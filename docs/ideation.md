We want to make a simple android app that sends a wakeonlan magic packet to an approved network.

If we're not connected to a wlan then only a message saying 'you have to connect to a wlan network' should be active and a settings menu allowing the user to add or remove network ssid's from the 'home network' group.

If the user is connected to an approved network then they should have the following options
- wake all machines on this network
- Wake specific machines
  - machine 1
  - machine 2
  - etc
- Add machine to wake

Adding a machine should require the user to input that machine's mac address, the app should then register that mac address with the connected wlan network. 
