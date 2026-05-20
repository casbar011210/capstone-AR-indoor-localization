from sqlalchemy import create_engine, Column, Integer, String, DateTime
from sqlalchemy.orm import declarative_base, sessionmaker
import datetime, os

Base = declarative_base()

class Scene(Base):
    __tablename__ = "scenes"
    id = Column(Integer, primary_key=True, autoincrement=True)
    name = Column(String, unique=True)
    model_path = Column(String)
    created_at = Column(DateTime, default=datetime.datetime.utcnow)

# The database location is fixed to the directory where the current script is located.
db_path = os.path.join(os.path.dirname(__file__), "vps.db")
engine = create_engine(f"sqlite:///{db_path}")
Base.metadata.create_all(engine)
Session = sessionmaker(bind=engine)

print("✅ Database initialized at:", db_path)
