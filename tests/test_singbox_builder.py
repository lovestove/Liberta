import unittest
from singbox_builder import parse_vless, build_config

class TestSingboxBuilder(unittest.TestCase):

    def test_parse_vless_reality(self):
        link = "vless://uuid@host.com:443?type=tcp&security=reality&sni=sni.com&pbk=public_key&sid=short_id#name"
        parsed = parse_vless(link)
        self.assertEqual(parsed["uuid"], "uuid")
        self.assertEqual(parsed["host"], "host.com")
        self.assertEqual(parsed["port"], 443)
        self.assertEqual(parsed["type"], "tcp")
        self.assertEqual(parsed["security"], "reality")
        self.assertEqual(parsed["sni"], "sni.com")
        self.assertEqual(parsed["pbk"], "public_key")
        self.assertEqual(parsed["sid"], "short_id")

    def test_parse_vless_invalid_link(self):
        link = "invalid://link"
        with self.assertRaises(ValueError):
            parse_vless(link)

    def test_build_config_reality(self):
        parsed = {
            "uuid": "uuid",
            "host": "host.com",
            "port": 443,
            "type": "tcp",
            "security": "reality",
            "sni": "sni.com",
            "pbk": "public_key",
            "sid": "short_id",
            "path": ""
        }
        config = build_config(parsed)
        outbound = config["outbounds"][0]
        self.assertEqual(outbound["type"], "vless")
        self.assertEqual(outbound["server"], "host.com")
        self.assertEqual(outbound["server_port"], 443)
        self.assertEqual(outbound["uuid"], "uuid")
        self.assertEqual(outbound["network"], "tcp")
        self.assertTrue(outbound["tls"]["enabled"])
        self.assertEqual(outbound["tls"]["server_name"], "sni.com")
        self.assertTrue(outbound["tls"]["reality"]["enabled"])
        self.assertEqual(outbound["tls"]["reality"]["public_key"], "public_key")
        self.assertEqual(outbound["tls"]["reality"]["short_id"], "short_id")

    def test_build_config_tls(self):
        parsed = {
            "uuid": "uuid",
            "host": "host.com",
            "port": 443,
            "type": "ws",
            "security": "tls",
            "sni": "sni.com",
            "pbk": "",
            "sid": "",
            "path": "/path"
        }
        config = build_config(parsed)
        outbound = config["outbounds"][0]
        self.assertEqual(outbound["type"], "vless")
        self.assertEqual(outbound["network"], "ws")
        self.assertTrue(outbound["tls"]["enabled"])
        self.assertEqual(outbound["tls"]["server_name"], "sni.com")
        self.assertIsNone(outbound["tls"].get("reality"))

if __name__ == "__main__":
    unittest.main()
