#!/usr/bin/env python3
"""Simple HTTP server for Block Dude 2 Level Editor"""

import http.server
import json
import os
from urllib.parse import urlparse

PORT = 8000
LEVELS_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'levels')

class LevelEditorHandler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=os.path.dirname(os.path.abspath(__file__)), **kwargs)

    def do_GET(self):
        parsed = urlparse(self.path)

        if parsed.path == '/api/levels':
            self.send_levels_list()
        elif parsed.path.startswith('/api/levels/'):
            level_num = parsed.path.split('/')[-1]
            self.send_level(level_num)
        else:
            super().do_GET()

    def do_POST(self):
        if self.path == '/api/levels':
            content_length = int(self.headers['Content-Length'])
            post_data = self.rfile.read(content_length)
            self.save_level(post_data)
        else:
            self.send_error(404)

    def do_DELETE(self):
        if self.path.startswith('/api/levels/'):
            level_num = self.path.split('/')[-1]
            self.delete_level(level_num)
        else:
            self.send_error(404)

    def send_levels_list(self):
        """Return list of all saved levels"""
        levels = []
        if os.path.exists(LEVELS_DIR):
            for filename in sorted(os.listdir(LEVELS_DIR)):
                if filename.endswith('.json'):
                    filepath = os.path.join(LEVELS_DIR, filename)
                    try:
                        with open(filepath, 'r') as f:
                            level_data = json.load(f)
                            levels.append({
                                'id': level_data.get('id'),
                                'name': level_data.get('name', 'Unnamed'),
                                'width': level_data.get('width', 16),
                                'height': level_data.get('height', 16),
                                'filename': filename
                            })
                    except (json.JSONDecodeError, IOError):
                        pass

        self.send_response(200)
        self.send_header('Content-Type', 'application/json')
        self.send_header('Access-Control-Allow-Origin', '*')
        self.end_headers()
        self.wfile.write(json.dumps(levels).encode())

    def send_level(self, level_num):
        """Return a specific level by number"""
        filename = f'level_{int(level_num):02d}.json'
        filepath = os.path.join(LEVELS_DIR, filename)

        if os.path.exists(filepath):
            with open(filepath, 'r') as f:
                self.send_response(200)
                self.send_header('Content-Type', 'application/json')
                self.send_header('Access-Control-Allow-Origin', '*')
                self.end_headers()
                self.wfile.write(f.read().encode())
        else:
            self.send_error(404, 'Level not found')

    def save_level(self, data):
        """Save a level to disk"""
        try:
            level_data = json.loads(data)
            level_id = level_data.get('id', 1)
            filename = f'level_{int(level_id):02d}.json'
            filepath = os.path.join(LEVELS_DIR, filename)

            os.makedirs(LEVELS_DIR, exist_ok=True)

            with open(filepath, 'w') as f:
                json.dump(level_data, f, indent=2)

            self.send_response(200)
            self.send_header('Content-Type', 'application/json')
            self.send_header('Access-Control-Allow-Origin', '*')
            self.end_headers()
            self.wfile.write(json.dumps({'success': True, 'filename': filename}).encode())
            print(f'Saved: {filepath}')
        except (json.JSONDecodeError, IOError) as e:
            self.send_error(400, str(e))

    def delete_level(self, level_num):
        """Delete a level"""
        filename = f'level_{int(level_num):02d}.json'
        filepath = os.path.join(LEVELS_DIR, filename)

        if os.path.exists(filepath):
            os.remove(filepath)
            self.send_response(200)
            self.send_header('Content-Type', 'application/json')
            self.send_header('Access-Control-Allow-Origin', '*')
            self.end_headers()
            self.wfile.write(json.dumps({'success': True}).encode())
            print(f'Deleted: {filepath}')
        else:
            self.send_error(404, 'Level not found')

    def do_OPTIONS(self):
        self.send_response(200)
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, DELETE, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type')
        self.end_headers()

if __name__ == '__main__':
    os.chdir(os.path.dirname(os.path.abspath(__file__)))

    with http.server.HTTPServer(('', PORT), LevelEditorHandler) as httpd:
        print(f'Block Dude 2 Level Editor')
        print(f'Open http://localhost:{PORT} in your browser')
        print(f'Levels saved to: {LEVELS_DIR}')
        print(f'Press Ctrl+C to stop')
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print('\nServer stopped')
