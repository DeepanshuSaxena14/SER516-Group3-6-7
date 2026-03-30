import { describe, it, expect, vi, afterEach } from "vitest";
import { deletStatbyId } from "../../mongo/controllers/CrudController.js";
import Stat from "../../mongo/models/StatModel.js";

vi.mock("../../mongo/models/StatModel.js");

describe("deletStatbyId controller", () => {
  const mockRecord = {
    _id: "507f1f77bcf86cd799439011",
    workCapacity: 40,
    velocity: 32,
  };

  const req = { params: { id: "507f1f77bcf86cd799439011" } };

  const json = vi.fn();
  const status = vi.fn(() => ({ json }));
  const res = { status };

  afterEach(() => {
    vi.clearAllMocks();
  });

  it("should delete a stat and return 200", async () => {
    Stat.findByIdAndDelete.mockResolvedValue(mockRecord);

    await deletStatbyId(req, res);

    expect(status).toHaveBeenCalledWith(200);
    expect(json).toHaveBeenCalledWith(mockRecord);
  });

  it("should return 404 if stat not found", async () => {
    Stat.findByIdAndDelete.mockResolvedValue(null);

    await deletStatbyId(req, res);

    expect(status).toHaveBeenCalledWith(404);
    expect(json).toHaveBeenCalledWith({ error: "Stat not found" });
  });

  it("should return 500 if an error occurs", async () => {
    Stat.findByIdAndDelete.mockRejectedValue(new Error("DB Error"));

    await deletStatbyId(req, res);

    expect(status).toHaveBeenCalledWith(500);
    expect(json).toHaveBeenCalledWith({ error: "DB Error" });
  });
});
