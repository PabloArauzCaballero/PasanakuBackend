from campania_lib import llamar, login
t = login("USR1")
est, r = llamar("GET", "/organizadores/d98ba56d-0cf1-4576-8cf5-09312ec9b97b/habilitacion", token=t)
print("habilitacion:", r)
