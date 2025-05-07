from fastapi import FastAPI, HTTPException
import xgboost
import numpy as np
import json
from pydantic import BaseModel
from typing import List, Optional
from utils import feature_engineering, check_virustotal_api

class AppData(BaseModel):
    package_name: str
    app_name: str
    permissions: List[str]
    sha256: Optional[str] = None
    app_size: Optional[int] = None
    install_source: Optional[str] = None
    app_category: Optional[str] = None

class RiskPrediction(BaseModel):
    risk_score: float
    risk_label: str
    virustotal_status: Optional[str] = None

app = FastAPI(
    title="CyberGuardian ML Service",
    description="ML prediction service for Android application risk assessment",
    version="1.0.0"
)

# Load the pre-trained XGBoost model
model = xgboost.Booster()
model.load_model("model.pkl")

@app.get("/")
async def root():
    return {"message": "CyberGuardian ML Service is running"}

@app.post("/predict", response_model=RiskPrediction)
async def predict(app_data: AppData):
    try:
        # Extract features from the received data
        permissions = app_data.permissions
        
        # Feature engineering (moved to utils)
        features = feature_engineering(permissions)
        
        # Predict risk using the model
        risk_score = model.predict(xgboost.DMatrix(features.reshape(1, -1)))
        risk_score_value = float(risk_score[0])
        risk_label = "suspicious" if risk_score_value > 0.5 else "benign"
        
        # Optional: Check VirusTotal API if sha256 is provided
        virustotal_status = None
        if app_data.sha256:
            virustotal_status = check_virustotal_api(app_data.sha256)
        
        return {
            "risk_score": risk_score_value,
            "risk_label": risk_label,
            "virustotal_status": virustotal_status
        }
    
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Prediction error: {str(e)}")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
