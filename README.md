# Discounter

> Manage and calculate discounts on baskets of items

Thinking about [this](http://codekata.com/kata/kata01-supermarket-pricing/) problems, 
while using clojure for the first time.

## Setup 

This repo is written in clojure and wrapped in nix with direnv.

## Use 

TBC

## Initial thoughts 

It does seem like a tricky problem to fully solve. 
There are many cases that should be considered.
A bit hard to get numbers on what offers exist. 
[This article](https://theconversation.com/supermarket-price-deals-the-good-the-bad-and-the-ugly-40703)
claims 

> The most prevalent types of deal are “Price Off” (eg “30% off” or “$1 off” normal price), 
> which accounts for around around 25% of all offers. 
> This is closely followed by “multi buy” or “X for $Y” deals, representing around 19% of all offers. 

But, heavy caveat, it does not back up this claim with any valid references.
(It points at an entire pay-walled journal and a now rescinded article.)

Here are some examples of scenarios we might face.

### Mutually exclusive applications 

Suppose we have a 3 for 2 offer (get the cheapest of 3 free) 
and a basket with 4 (distinct) items that it can be applied to. 
There are 4 mutually exclusive possible discounts. 
The optimal strategy is to apply the offer to the 3 most expensive,
and thus get the 2nd cheapest free. All other options result in getting the cheapest free.

### Conflicting offers 

Suppose there are two offers 

1. 30% off
2. 3 for 2

If the basket contains 3 items of which the above offers apply then the optimal application depends on the prices of the goods. 
For example, if the 3 items are of the same price, 3 for 2 is equivalent to a 1/3 off which is  a better deal than 30% off. 
However if two are equally priced while the third is 50%, then 3 for 2 is equivalent to ~28% off. 
 
### Partially conflicting applications 

Suppose there are two offers 

1. 30% off premium items
2. 3 for 2

If the basket contains 4 items to which the 3 for 2 offer applies,
and 30% off only the most expensive item, then the optimal application again depends on the relative pricing between the items. 
More precisely, if the prices are `(p0 p1 p2 p3)` in increasing order, then the two strategies have the following results.
```math
	strat-both => (p0 + p3/3) ; strat-3-for-2 => (p1)
```

### Global discounts

Consider an offer of the form _$5 off when you spend over $20_. 
The logic is based on the whole basket, not to specific items within it. 
This could be with or without an offer exclusivity clause. 

In the non-exclusive case, then it is ambiguous whether this means $20 before the application of other offers,  or after. 
If is the latter, then the will be scenarios where the application of a, say "3 for 2 on item X" reduces a basket
from just over $20 to just under $20, and losing the better $5 discount. 

In the exclusive case, it is ambiguous as to whether:

- offers can be applied and that it is applicable so long as the total cost of undiscounted items is > $20, or
- there can be no other discounts at all

### Combo offers 

Suppose we have the offers 

- Meal deal for $3
- 3 for 2 on fizzy pop

TODO ?

### Exclusive and non-exclusive offers 

Suppose there are two offers where one is exclusive and the other non-exclusive. 
The second cannot be applied where the first already has been. 
Thus the logic governing the application of the second must be aware of the logic of the first. 

### Specific item reduced

Suppose an item is reduced not because of a product wide offer, 
but because the specific item is some how defective (eg box torn, or soon to expire). 

This could be treated as an additional offer.
Since this is determined at scanning time, this would be additional data appearing as input with the basket. 

## Design

The retailer retails products. 
Each product has a unique id, unique name, cost per unit, units of measure (eg item or gram), and a pretty name. 

A basket is a list of items: consisting of a product with a quantity. 
A product may appear multiple times in the basket list, as distinct items. 

To a basket we can iteratively apply offers. 
The application of an offer results either in nothing or some discount. 
Whether or not an offer has any effect depends on the basket, and the existing list of discounts. 
A discount is the application of an offer to a subset (possibly all or none) of the items in the basket.

The order of applying offers may matter. 
In addition a single order might have multiple, mutually exclive applications. 
What this is the best strategy? 

### Assumptions

We make the following assumptions

1. All valid states can be reach by repeated applications of offers 
where each discount is some money off the total spent.
2. Applying orders is additive: it does not modify existing discounts. 
A new discount is appended on to existing discounts.
3. The order of items in the basket has not effect on the possible discounts.
4. The order of the existing discounts has no effect on additional possible discounts.

The assumptions imply that the process will terminate from any (finite) starting point.

By the assumptions, the states (both `basket` and `discounts`)
 are treated as unordered lists. 

An `offer-options` is a function 
```
	options : {Baskets} x {Discounts} --> {Discounts}
```
Note that this is not the same as the order of application is inconsequential.

### Strategies 

#### Search all

By the assumption, we could visit all possible terminal states and select the best outcome. 
This might be quite expensive if not done in a smart way. 

#### First come first served

This is surely easier: 
give offers an ordering and apply one repeatedly until there are no more discounts, 
and then move on to the next until complete.

As the described in the examples section, this will result in non-optimal outcomes for clients, 
but is much easier to reason about. 

#### Fcfs with exclusivity

Expanding the context when applying an offer we could handle exclusivity case. (to be expanded upon)

## TODOs 

- Many more tests 
- Implement more offer types 
- System to manage offers / read in from file 
- Handle exclusivity (use a `discount` tracking which items it uses and whether or not it is exclusive.)
- Use clojure specs