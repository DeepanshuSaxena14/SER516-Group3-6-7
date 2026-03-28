import { describe, it, expect, vi, afterEach } from "vitest";
import { createFocusFactor } from "../../mongo/controllers/CrudController.js"
import Stat from "../../mongo/models/StatModel.js";

vi.mock("../../mongo/models/StatModel.js");

describe("createStat controller", () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it("should create a stat and return 201", async () => {

    const req = {
      body: {
        velocity: 40,
        workCapacity: 50
      }
    };

    const json = vi.fn();
    const status = vi.fn(() => ({ json }));

    const res = {
      status
    };

    const savedStat = {
      _id: "123",
      velocity: 40,
      workCapacity: 50
    };

    Stat.mockImplementation(function () {
      this.save = vi.fn().mockResolvedValue(savedStat);
    });

    await createFocusFactor(req, res);

    expect(status).toHaveBeenCalledWith(201);
    expect(json).toHaveBeenCalledWith(savedStat);
  });

  it("should return 500 if error occurs", async () => {

    const req = {
      body: {
        velocity: 40,
        workCapacity: 50
      }
    };

    const json = vi.fn();
    const status = vi.fn(() => ({ json }));

    const res = {
      status
    };

    Stat.mockImplementation(function () {
      this.save = vi.fn().mockRejectedValue(new Error("DB Error"));
    });

    await createFocusFactor(req, res);

    expect(status).toHaveBeenCalledWith(500);
    expect(json).toHaveBeenCalledWith({ error: "DB Error" });
  });

});
