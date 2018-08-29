## Moolah plugin
Add fake money to IRC channel, with a bank to track each users' funds

## Features
* Simple balance tracking
* Simple gambling
* Money transfer to other users
* Money mining

## Commands
* .bank - main command, takes subcommands:
* .bank balance (user) - prints out current balance
* .bank transfer [user] [amount] - allows transferring money to another user of the bank
* .bank slots [2|5|10|20] - Simple 3-slot slot machine, no user interaction
* .bank hilo [hi(gh)|lo(w)|eq(ual)] [100|200|1000] - Simple higher/lower/equal than 5 bet on a random number between 0 and 11
* .bank mine - Mines money based on length of time since last mining attempt and "richness" of current vein

## Mechanics

### Slots
* Performs 3x random picks of a set of slot results and yields money based on the bet and the resul
* Images include some utf-8 characters, like 🍒, $, 7, 🔔, 🍋
* If bar in any slot but bar not in every slot, lose
* If bar in every slot, payout 2x
* If 7 in any slot, payout +1.1x for every 7
* If 7 in every slot, payout 7x
* For any other symbol, 2 matching adjacent (no splits) yields 1.2x, matching 3 yields 2x + symbol bonus
* Symbol bonuses: $,5 = 1x, 🍒,🍋 = 2x, 🔔 = 3x

### Hilo
* User bets on hi, lo, or eq to 5 on 0-11
* Hi/lo yields 2x, eq yields 5x
* hi/lo lose on opposite or 5, eq loses on != 5


### Mine
* Every so often (random amount of time between 30 minutes and 3 hours) the mine is refreshed
* Refresh selects a random "richness" between 0.1 and 10 (normal distribution centered on 1)
* On a mine command, yield (#30 minute blocks since last mine/48, max 1.0) * richness
* Richness is not available to user, nor last user mine time
