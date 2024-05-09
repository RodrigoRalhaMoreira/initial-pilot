import hashlib

# Example usage
public_address = "0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000549282910E81313D0a0975EEdDe05B23d5f6Ae08"

x = bytes.fromhex(public_address)

hash = hashlib.sha256(x)
hx = hash.hexdigest()
print("hx: ", hx)

hh = bytearray(hash.digest())
i1 = hh[:16]
i2 = hh[16:]

val1 = int.from_bytes(i1, "big")
val2 = int.from_bytes(i2, "big")
print(val1)
print(val2)
print()

print(hex(val1))
print(hex(val2))


def split_and_convert(public_address):
    # Pad the public address with leading zeros to ensure it has 128 characters
    public_address = public_address.zfill(128)

    # Split the string into 4 equal parts
    parts = [public_address[i : i + 32] for i in range(0, len(public_address), 32)]

    return [int(part, 16) for part in parts]


print(split_and_convert(public_address))
