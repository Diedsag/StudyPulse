from flask import Flask
from flask_cors import CORS
from flask_sqlalchemy import SQLAlchemy

from .config import Config

db = SQLAlchemy()


def create_app():
    app = Flask(__name__)
    app.config.from_object(Config)
    app.json.ensure_ascii = False

    CORS(app)
    db.init_app(app)

    from . import models  # noqa: F401
    from .api.health import health_bp
    from .api.devices import devices_bp
    from .api.packages import packages_bp
    from .api.sync import sync_bp
    from .api.stats import stats_bp

    app.register_blueprint(health_bp)
    app.register_blueprint(devices_bp, url_prefix="/api/devices")
    app.register_blueprint(packages_bp, url_prefix="/api/packages")
    app.register_blueprint(sync_bp, url_prefix="/api/sync")
    app.register_blueprint(stats_bp, url_prefix="/api")

    with app.app_context():
        db.create_all()

    return app