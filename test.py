#!/usr/bin/env python
import random, math

numnums = 11
results = [0 for i in range(numnums)]

num = 100000
for i in range(num):
    results[int(random.random()*numnums)] += 1

print(', '.join('{:d}: {:.2f}%'.format(i, float(val)/num*100) for i, val in enumerate(results)))
