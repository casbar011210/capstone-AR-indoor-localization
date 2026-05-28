from sqlalchemy import create_engine, Column, Integer, String, DateTime
from sqlalchemy.orm import declarative_base, sessionmaker
import datetime
import os

# Create the base class for database models
Base = declarative_base()


class Scene(Base):
    """
    Database table used to store reconstructed scene information.
    """

    __tablename__ = "scenes"

    # Unique scene identifier
    id = Column(Integer, primary_key=True, autoincrement=True)

    # Scene name
    name = Column(String, unique=True)

    # Path to the reconstructed SfM model
    model_path = Column(String)

    # Scene creation timestamp
    created_at = Column(DateTime, default=datetime.datetime.utcnow)


# Define the SQLite database path
db_path = os.path.join(os.path.dirname(__file__), "vps.db")

# Create SQLite engine
engine = create_engine(f"sqlite:///{db_path}")

# Create all database tables
Base.metadata.create_all(engine)

# Create database session factory
Session = sessionmaker(bind=engine)

print("Database initialized at:", db_path)
