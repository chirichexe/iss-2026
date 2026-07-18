import re
# checking prolog syntax rules. atoms must start with a lower case letter and contain only letters, digits, and underscores.
match = re.match(r'^[a-z][a-zA-Z0-9_]*$', 'esp32_sonar')
print("Is esp32_sonar valid atom?", bool(match))
