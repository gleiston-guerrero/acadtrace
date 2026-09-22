"""Offline checks: the operational script must never contact production in tests."""
import contextlib
import io
import os
from pathlib import Path
import runpy
import unittest
from unittest.mock import patch

SCRIPT = Path(__file__).with_name('populate_all_docentes_full.py')


class ApiConfigurationTest(unittest.TestCase):
    def execute(self, env):
        response = io.BytesIO(b'{}')
        with patch.dict(os.environ, env, clear=True), \
             patch('urllib.request.urlopen', return_value=response) as request, \
             contextlib.redirect_stdout(io.StringIO()):
            runpy.run_path(str(SCRIPT), run_name='__main__')
            return request.call_args.args[0]

    def test_external_endpoint(self):
        request = self.execute({'SGA_API_BASE_URL': 'https://api.example.invalid',
                                'SGA_ADMIN_USERNAME': 'synthetic-user',
                                'SGA_ADMIN_PASSWORD': 'synthetic-password'})
        self.assertEqual(request.full_url, 'https://api.example.invalid/api/auth/login')

    def test_local_default(self):
        request = self.execute({'SGA_ADMIN_USERNAME': 'synthetic-user',
                                'SGA_ADMIN_PASSWORD': 'synthetic-password'})
        self.assertEqual(request.full_url, 'http://localhost:8080/api/auth/login')

    def test_missing_credentials_fail_before_network(self):
        with patch('urllib.request.urlopen') as request:
            with self.assertRaises(SystemExit):
                self.execute({})
            request.assert_not_called()

    def test_reject_embedded_credentials(self):
        with self.assertRaises(SystemExit):
            self.execute({'SGA_API_BASE_URL': 'https://user:synthetic@api.example.invalid'})


if __name__ == '__main__':
    unittest.main()
